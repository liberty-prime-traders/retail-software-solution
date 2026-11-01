package me.ezra_home.retail_software_solution.locations.product_sales_report

import me.ezra_home.retail_software_solution.configuration.session.LocationSession
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.DateSoldRange
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportQueryBuilder
import me.ezra_home.retail_software_solution.locations.business.product_sales_report.ProductSalesReportWindow
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// Executes the SQL the builder emits against Postgres (the adjustment aggregate is LEFT JOIN LATERAL, which H2
// cannot run). A line with two adjustments must contribute its gross, cogs and quantity exactly once.
class ProductSalesReportQueryBuilderPostgresTest {

    private lateinit var connection: Connection

    private val productId = UUID.randomUUID()
    private val otherProductId = UUID.randomUUID()
    private val cartonUnitId = UUID.randomUUID()
    private val pieceUnitId = UUID.randomUUID()
    private val reasonId = UUID.randomUUID()

    private val rangeFrom = OffsetDateTime.parse("2030-03-01T00:00:00Z")
    private val rangeBefore = OffsetDateTime.parse("2030-04-01T00:00:00Z")
    private val dateRange = DateSoldRange(rangeFrom, rangeBefore)

    private val twoAdjustmentLineId = UUID.randomUUID()
    private val plainLineId = UUID.randomUUID()

    @BeforeEach
    fun createSchemaAndFixtures() {
        connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        connection.createStatement().use { statement ->
            statement.execute("DROP SCHEMA IF EXISTS product_sales_report_test CASCADE")
            statement.execute("CREATE SCHEMA product_sales_report_test")
            statement.execute("SET search_path TO product_sales_report_test")
            statement.execute(
                """CREATE TABLE sale (id UUID PRIMARY KEY, reference_number VARCHAR(30), status VARCHAR(5),
                   date_sold TIMESTAMP WITH TIME ZONE)"""
            )
            statement.execute(
                """CREATE TABLE sale_line (id UUID PRIMARY KEY, sale_id UUID, location_product_id UUID, unit_id UUID,
                   user_provided_quantity NUMERIC(19,4), storage_units_per_user_provided_unit BIGINT,
                   line_total NUMERIC(19,4), cogs_total NUMERIC(19,4))"""
            )
            statement.execute(
                """CREATE TABLE sale_adjustment (id UUID PRIMARY KEY, sale_id UUID, sale_line_id UUID, direction VARCHAR(5),
                   calculated_amount NUMERIC(19,4), adjustment_reason_id UUID, created_on TIMESTAMP WITH TIME ZONE)"""
            )
        }

        val twoAdjustmentSaleId = insertSale("SAL1", "CFM", "2030-03-20T10:00:00Z")
        insertLine(twoAdjustmentLineId, twoAdjustmentSaleId, productId, cartonUnitId, "2", 12, "100.00", "60.00")
        insertAdjustment(twoAdjustmentSaleId, twoAdjustmentLineId, "DISC", "10.00", 1)
        insertAdjustment(twoAdjustmentSaleId, twoAdjustmentLineId, "SRCH", "5.00", 2)
        insertAdjustment(twoAdjustmentSaleId, null, "DISC", "7.00", 3)

        val plainSaleId = insertSale("SAL2", "CFM", "2030-03-10T10:00:00Z")
        insertLine(plainLineId, plainSaleId, productId, pieceUnitId, "6", 1, "30.00", null)

        insertLine(UUID.randomUUID(), insertSale("SAL3", "VD", "2030-03-15T10:00:00Z"), productId, pieceUnitId, "1", 1, "999.00", "1.00")
        insertLine(UUID.randomUUID(), insertSale("SAL4", "DFT", "2030-03-16T10:00:00Z"), productId, pieceUnitId, "1", 1, "999.00", null)
        insertLine(UUID.randomUUID(), insertSale("SAL5", "CFM", "2030-04-01T00:00:00Z"), productId, pieceUnitId, "1", 1, "999.00", "1.00")
        insertLine(UUID.randomUUID(), insertSale("SAL6", "CFM", "2030-02-28T23:59:59Z"), productId, pieceUnitId, "1", 1, "999.00", "1.00")
        insertLine(UUID.randomUUID(), insertSale("SAL7", "CFM", "2030-03-12T10:00:00Z"), otherProductId, pieceUnitId, "1", 1, "999.00", "1.00")
    }

    @AfterEach
    fun closeConnection() {
        connection.close()
    }

    @Test
    fun `summary counts each line once however many adjustments it has`() {
        val predicate = ProductSalesReportQueryBuilder.buildPredicate(listOf(productId), dateRange)

        val rows = execute(ProductSalesReportQueryBuilder.buildSummaryQuery(predicate))

        assertEquals(1, rows.size)
        val row = rows.single()
        assertEquals(productId, row.getValue("location_product_id"))
        assertEquals(2L, (row.getValue("line_count") as Number).toLong())
        assertEquals(0, BigDecimal("30").compareTo(row.getValue("storage_quantity") as BigDecimal))
        assertEquals(0, BigDecimal("130").compareTo(row.getValue("gross_sales") as BigDecimal))
        assertEquals(0, BigDecimal("60").compareTo(row.getValue("cogs_total") as BigDecimal))
        assertEquals(0, BigDecimal("10").compareTo(row.getValue("discount_magnitude") as BigDecimal))
        assertEquals(0, BigDecimal("5").compareTo(row.getValue("surcharge_magnitude") as BigDecimal))
    }

    @Test
    fun `summary omits a product that sold nothing in range`() {
        val predicate = ProductSalesReportQueryBuilder.buildPredicate(listOf(UUID.randomUUID()), dateRange)

        assertEquals(emptyList<Map<String, Any?>>(), execute(ProductSalesReportQueryBuilder.buildSummaryQuery(predicate)))
    }

    @Test
    fun `lines are confirmed in-range sales of the product, newest first, and page by keyset`() {
        val predicate = ProductSalesReportQueryBuilder.buildPredicate(listOf(productId), dateRange)

        val firstPage = execute(ProductSalesReportQueryBuilder.buildLinesQuery(predicate, null), pageSize = 1)
        assertEquals(listOf(twoAdjustmentLineId), firstPage.map { it.getValue("sale_line_id") })

        val cursor = KeysetSearchCursor(OffsetDateTime.parse("2030-03-20T10:00:00Z"), twoAdjustmentLineId)
        val secondPage = execute(ProductSalesReportQueryBuilder.buildLinesQuery(predicate, cursor), pageSize = 5)
        assertEquals(listOf(plainLineId), secondPage.map { it.getValue("sale_line_id") })
    }

    @Test
    fun `line adjustments exclude order-level rows`() {
        val rows = execute(ProductSalesReportQueryBuilder.buildLineAdjustmentsQuery(listOf(twoAdjustmentLineId, plainLineId)))

        assertEquals(listOf("DISC", "SRCH"), rows.map { it.getValue("direction") })
    }

    @Test
    fun `all-products summary returns each product with a confirmed sale in range and no other`() {
        val voidedOnlyProductId = UUID.randomUUID()
        insertLine(UUID.randomUUID(), insertSale("SAL8", "VD", "2030-03-14T10:00:00Z"), voidedOnlyProductId, pieceUnitId, "1", 1, "10.00", "1.00")
        val predicate = ProductSalesReportQueryBuilder.buildAllProductsPredicate(dateRange)

        val rows = execute(ProductSalesReportQueryBuilder.buildSummaryQuery(predicate))

        assertEquals(setOf(productId, otherProductId), rows.map { it.getValue("location_product_id") }.toSet())
        val productRow = rows.single { it.getValue("location_product_id") == productId }
        assertEquals(0, BigDecimal("130").compareTo(productRow.getValue("gross_sales") as BigDecimal))
    }

    @Test
    fun `a window includes its first day and last day and excludes the day after`() {
        SessionContextProvider.setSession(
            SessionContext(
                organization = OrgSession(id = UUID.randomUUID(), schemaName = "org-a", timezone = "UTC"),
                location = LocationSession(id = UUID.randomUUID(), schemaName = "loc-1")
            )
        )
        try {
            val windowDateRange = ProductSalesReportWindow(LocalDate.of(2030, 3, 12), LocalDate.of(2030, 6, 10)).toDateRange()
            val oldestDayStartProductId = UUID.randomUUID()
            val dayBeforeWindowProductId = UUID.randomUUID()
            val todayEndProductId = UUID.randomUUID()
            val tomorrowStartProductId = UUID.randomUUID()
            insertLine(UUID.randomUUID(), insertSale("W1", "CFM", "2030-03-12T00:00:00Z"), oldestDayStartProductId, pieceUnitId, "1", 1, "10.00", "1.00")
            insertLine(UUID.randomUUID(), insertSale("W2", "CFM", "2030-03-11T23:59:59Z"), dayBeforeWindowProductId, pieceUnitId, "1", 1, "10.00", "1.00")
            insertLine(UUID.randomUUID(), insertSale("W3", "CFM", "2030-06-09T23:59:59Z"), todayEndProductId, pieceUnitId, "1", 1, "10.00", "1.00")
            insertLine(UUID.randomUUID(), insertSale("W4", "CFM", "2030-06-10T00:00:00Z"), tomorrowStartProductId, pieceUnitId, "1", 1, "10.00", "1.00")
            val predicate = ProductSalesReportQueryBuilder.buildAllProductsPredicate(windowDateRange)

            val rows = execute(ProductSalesReportQueryBuilder.buildSummaryQuery(predicate))

            val boundaryProductIds = setOf(oldestDayStartProductId, dayBeforeWindowProductId, todayEndProductId, tomorrowStartProductId)
            assertEquals(
                setOf(oldestDayStartProductId, todayEndProductId),
                rows.map { it.getValue("location_product_id") }.filter { it in boundaryProductIds }.toSet()
            )
        } finally {
            SessionContextProvider.clear()
        }
    }

    @Test
    fun `daily summary groups sales by the calendar day in the given zone`() {
        insertLine(UUID.randomUUID(), insertSale("D1", "CFM", "2030-03-21T03:00:00Z"), productId, pieceUnitId, "1", 1, "7.00", "1.00")
        val predicate = ProductSalesReportQueryBuilder.buildAllProductsPredicate(dateRange)

        fun grossSalesByDay(zone: String) = execute(ProductSalesReportQueryBuilder.buildDailySummaryQuery(predicate, zone))
            .filter { it.getValue("location_product_id") == productId }
            .associate { it.getValue("sold_on") to (it.getValue("gross_sales") as BigDecimal).toInt() }

        assertEquals(mapOf<Any?, Int>("2030-03-10" to 30, "2030-03-20" to 100, "2030-03-21" to 7), grossSalesByDay("UTC"))
        assertEquals(mapOf<Any?, Int>("2030-03-10" to 30, "2030-03-20" to 107), grossSalesByDay("America/Chicago"))
    }

    @Test
    fun `daily rows add up to the range summary`() {
        val predicate = ProductSalesReportQueryBuilder.buildAllProductsPredicate(dateRange)

        val dailyRows = execute(ProductSalesReportQueryBuilder.buildDailySummaryQuery(predicate, "UTC"))
            .filter { it.getValue("location_product_id") == productId }
        val rangeRow = execute(ProductSalesReportQueryBuilder.buildSummaryQuery(predicate))
            .single { it.getValue("location_product_id") == productId }

        listOf("gross_sales", "cogs_total", "discount_magnitude", "surcharge_magnitude", "storage_quantity").forEach { column ->
            val dailyTotal = dailyRows.sumOf { it.getValue(column) as BigDecimal }
            assertEquals(0, (rangeRow.getValue(column) as BigDecimal).compareTo(dailyTotal), column)
        }
        assertEquals(rangeRow.getValue("line_count"), dailyRows.sumOf { (it.getValue("line_count") as Number).toLong() })
    }

    private fun insertSale(referenceNumber: String, status: String, dateSold: String): UUID {
        val saleId = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO sale (id, reference_number, status, date_sold) VALUES (?, ?, ?, ?)").use { statement ->
            statement.setObject(1, saleId)
            statement.setString(2, referenceNumber)
            statement.setString(3, status)
            statement.setObject(4, OffsetDateTime.parse(dateSold))
            statement.executeUpdate()
        }
        return saleId
    }

    private fun insertLine(
        saleLineId: UUID,
        saleId: UUID,
        locationProductId: UUID,
        unitId: UUID,
        userProvidedQuantity: String,
        storageUnitsPerUserProvidedUnit: Long,
        lineTotal: String,
        cogsTotal: String?
    ) {
        connection.prepareStatement(
            """INSERT INTO sale_line (id, sale_id, location_product_id, unit_id, user_provided_quantity,
               storage_units_per_user_provided_unit, line_total, cogs_total) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"""
        ).use { statement ->
            statement.setObject(1, saleLineId)
            statement.setObject(2, saleId)
            statement.setObject(3, locationProductId)
            statement.setObject(4, unitId)
            statement.setBigDecimal(5, BigDecimal(userProvidedQuantity))
            statement.setLong(6, storageUnitsPerUserProvidedUnit)
            statement.setBigDecimal(7, BigDecimal(lineTotal))
            statement.setBigDecimal(8, cogsTotal?.let { BigDecimal(it) })
            statement.executeUpdate()
        }
    }

    private fun insertAdjustment(saleId: UUID, saleLineId: UUID?, direction: String, calculatedAmount: String, createdOnSecond: Int) {
        connection.prepareStatement(
            """INSERT INTO sale_adjustment (id, sale_id, sale_line_id, direction, calculated_amount, adjustment_reason_id, created_on)
               VALUES (?, ?, ?, ?, ?, ?, ?)"""
        ).use { statement ->
            statement.setObject(1, UUID.randomUUID())
            statement.setObject(2, saleId)
            statement.setObject(3, saleLineId)
            statement.setString(4, direction)
            statement.setBigDecimal(5, BigDecimal(calculatedAmount))
            statement.setObject(6, reasonId)
            statement.setObject(7, OffsetDateTime.parse("2030-03-01T00:00:0${createdOnSecond}Z"))
            statement.executeUpdate()
        }
    }

    private fun execute(sqlQuery: SqlQuery, pageSize: Int? = null): List<Map<String, Any?>> {
        val parameterNames = mutableListOf<String>()
        val positionalSql = NAMED_PARAMETER.replace(sqlQuery.sql) { match ->
            parameterNames.add(match.groupValues[1])
            "?"
        }
        val parameters = if (pageSize == null) sqlQuery.params else sqlQuery.params + (QueryParameterNames.PAGE_SIZE to pageSize)
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
            is Array<*> -> statement.setArray(index, connection.createArrayOf("uuid", value))
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

        val NAMED_PARAMETER = Regex("(?<!:):([A-Za-z][A-Za-z0-9_]*)")
    }
}
