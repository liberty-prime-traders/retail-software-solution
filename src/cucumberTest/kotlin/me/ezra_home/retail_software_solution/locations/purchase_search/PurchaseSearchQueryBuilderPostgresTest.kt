package me.ezra_home.retail_software_solution.locations.purchase_search

import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseLineEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseSearchParameters
import me.ezra_home.retail_software_solution.locations.business.purchase.search.PurchaseSearchQueryBuilder
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.OffsetDateTime
import java.util.UUID

// Executes the SQL the builder actually emits against Postgres (the ordered-total join is LEFT JOIN LATERAL, which H2
// cannot run), so the expression is checked on rows and against PurchaseLineEntity.getTotalCost().
class PurchaseSearchQueryBuilderPostgresTest {

    private lateinit var connection: Connection

    private val supplierId = UUID.randomUUID()
    private val createdOn = OffsetDateTime.parse("2030-01-01T00:00:00Z")

    private val partiallyCanceledLine = line(unitCost = "5.00", ordered = "10", storageUnitsPerUserProvidedUnit = 12L, canceled = "36")
    private val oddlyPricedLine = line(unitCost = "3.3333", ordered = "7", storageUnitsPerUserProvidedUnit = 3L, canceled = "5")
    private val untouchedLine = line(unitCost = "2.50", ordered = "4", storageUnitsPerUserProvidedUnit = 6L, canceled = "0")

    @BeforeEach
    fun createSchemaAndFixtures() {
        connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        connection.createStatement().use { statement ->
            statement.execute("DROP SCHEMA IF EXISTS purchase_search_test CASCADE")
            statement.execute("CREATE SCHEMA purchase_search_test")
            statement.execute("SET search_path TO purchase_search_test")
            statement.execute(
                """CREATE TABLE purchase (id UUID PRIMARY KEY, reference_number VARCHAR(30), supplier_id UUID, purchase_status VARCHAR(5),
                   payment_status VARCHAR(5), date_ordered TIMESTAMP WITH TIME ZONE, created_on TIMESTAMP WITH TIME ZONE)"""
            )
            statement.execute(
                """CREATE TABLE purchase_line (id UUID PRIMARY KEY, purchase_id UUID, unit_cost NUMERIC(19,4),
                   user_provided_quantity_ordered NUMERIC(19,4), storage_units_per_user_provided_unit BIGINT, storage_quantity_canceled NUMERIC(28,4))"""
            )
            statement.execute("CREATE TABLE supplier_payment_row (id UUID PRIMARY KEY, purchase_id UUID, amount NUMERIC(19,4))")
            statement.execute("CREATE VIEW active_supplier_payment AS SELECT id, purchase_id, amount FROM supplier_payment_row")
        }
        insertPurchase("PUR1", partiallyCanceledLine)
        insertPurchase("PUR2", oddlyPricedLine)
        insertPurchase("PUR3", untouchedLine)
        insertPurchase("PUR4")
    }

    @AfterEach
    fun closeConnection() {
        connection.close()
    }

    @Test
    fun `the sql ordered total equals the entity total for lines with and without a partial cancel`() {
        val entityTotals = listOf(partiallyCanceledLine, oddlyPricedLine, untouchedLine).map { it.getTotalCost() }

        val summary = summarize(PurchaseSearchParameters())

        assertEquals(0, entityTotals.fold(BigDecimal.ZERO, BigDecimal::add).compareTo(summary.single()["total_ordered"] as BigDecimal))
    }

    @Test
    fun `a partial cancel is priced at the expected quantity so the amount filter sees 35 and not the ordered 50`() {
        assertEquals(0, BigDecimal("35.00").compareTo(partiallyCanceledLine.getTotalCost()))

        assertEquals(listOf("PUR1"), references(list(PurchaseSearchParameters(minAmount = BigDecimal("35.00"), maxAmount = BigDecimal("35.00")))))
        assertEquals(emptyList<String>(), references(list(PurchaseSearchParameters(minAmount = BigDecimal("35.0001"), maxAmount = BigDecimal("49.00")))))
    }

    @Test
    fun `amount range is inclusive at the entity total of a line priced at a fractional cost`() {
        val total = oddlyPricedLine.getTotalCost()

        assertEquals(listOf("PUR2"), references(list(PurchaseSearchParameters(minAmount = total, maxAmount = total))))
        assertEquals(emptyList<String>(), references(list(PurchaseSearchParameters(minAmount = total.add(BigDecimal("0.0001")), maxAmount = total.add(BigDecimal("0.0002"))))))
    }

    @Test
    fun `a purchase with no lines counts as a zero total`() {
        assertEquals(listOf("PUR4"), references(list(PurchaseSearchParameters(maxAmount = BigDecimal.ZERO))))
    }

    private fun line(unitCost: String, ordered: String, storageUnitsPerUserProvidedUnit: Long, canceled: String) = PurchaseLineEntity(
        purchaseId = UUID.randomUUID(),
        locationProductId = UUID.randomUUID(),
        userProvidedQuantityOrdered = BigDecimal(ordered),
        unitCost = BigDecimal(unitCost),
        unitId = UUID.randomUUID(),
        storageUnitsPerUserProvidedUnit = storageUnitsPerUserProvidedUnit,
        storageQuantityCanceled = BigDecimal(canceled),
    ).also { it.id = UUID.randomUUID() }

    private fun insertPurchase(referenceNumber: String, vararg lines: PurchaseLineEntity) {
        val purchaseId = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO purchase VALUES (?, ?, ?, 'ORD', 'UNPD', ?, ?)").use { statement ->
            statement.setObject(1, purchaseId)
            statement.setString(2, referenceNumber)
            statement.setObject(3, supplierId)
            statement.setObject(4, createdOn)
            statement.setObject(5, createdOn)
            statement.executeUpdate()
        }
        lines.forEach { line ->
            connection.prepareStatement("INSERT INTO purchase_line VALUES (?, ?, ?, ?, ?, ?)").use { statement ->
                statement.setObject(1, line.id)
                statement.setObject(2, purchaseId)
                statement.setBigDecimal(3, line.unitCost)
                statement.setBigDecimal(4, line.userProvidedQuantityOrdered)
                statement.setLong(5, line.storageUnitsPerUserProvidedUnit)
                statement.setBigDecimal(6, line.storageQuantityCanceled)
                statement.executeUpdate()
            }
        }
    }

    private fun references(rows: List<Map<String, Any?>>) = rows.map { it["reference_number"] }.sortedBy { it.toString() }

    private fun list(purchaseSearchParameters: PurchaseSearchParameters): List<Map<String, Any?>> {
        val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
        return execute(PurchaseSearchQueryBuilder.buildListQuery(predicate, null), mapOf(PAGE_SIZE to 50))
    }

    private fun summarize(purchaseSearchParameters: PurchaseSearchParameters): List<Map<String, Any?>> {
        val predicate = PurchaseSearchQueryBuilder.buildPredicate(purchaseSearchParameters)
        return execute(PurchaseSearchQueryBuilder.buildPaymentStatusSummaryQuery(predicate), emptyMap())
    }

    private fun execute(sqlQuery: SqlQuery, extraParameters: Map<String, Any>): List<Map<String, Any?>> {
        val parameterNames = mutableListOf<String>()
        val positionalSql = NAMED_PARAMETER.replace(sqlQuery.sql) { match ->
            parameterNames.add(match.groupValues[1])
            "?"
        }
        val parameters = sqlQuery.params + extraParameters
        connection.prepareStatement(positionalSql).use { statement ->
            parameterNames.forEachIndexed { index, name -> bind(statement, index + 1, parameters.getValue(name)) }
            statement.executeQuery().use { resultSet ->
                val columnLabels = (1..resultSet.metaData.columnCount).map { resultSet.metaData.getColumnLabel(it).lowercase() }
                val rows = mutableListOf<Map<String, Any?>>()
                while (resultSet.next()) {
                    rows.add(columnLabels.mapIndexed { index, label -> label to resultSet.getObject(index + 1) }.toMap())
                }
                return rows
            }
        }
    }

    private fun bind(statement: PreparedStatement, index: Int, value: Any) {
        when (value) {
            is Array<*> -> statement.setArray(index, connection.createArrayOf(if (value.firstOrNull() is UUID) "uuid" else "varchar", value))
            else -> statement.setObject(index, value)
        }
    }

    private companion object {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16")

        @BeforeAll
        @JvmStatic
        fun startPostgres() {
            // Docker Desktop 29 reports a minimum supported API version of 1.44; see ContainerInitializer
            System.setProperty("api.version", "1.44")
            postgres.start()
        }

        @AfterAll
        @JvmStatic
        fun stopPostgres() {
            postgres.stop()
        }

        const val PAGE_SIZE = "pageSize"
        val NAMED_PARAMETER = Regex("(?<!:):([A-Za-z][A-Za-z0-9_]*)")
    }
}
