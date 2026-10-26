package me.ezra_home.retail_software_solution.organizations.ledger_movement

import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.organizations.business.ledger.LedgerEntryRepository
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerMovementReader
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.springframework.data.jpa.repository.Query
import org.testcontainers.containers.PostgreSQLContainer
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

// Runs the @Query text of LedgerEntryRepository (read by reflection, so it cannot drift) with the day-start literal
// LedgerMovementReader builds, because the timestamptz[] cast and width_bucket cannot run on H2.
class LedgerMovementQueriesPostgresTest {

    private lateinit var connection: Connection

    private val ledgerMovementReader = LedgerMovementReader(mock(LedgerEntryRepository::class.java))
    private val locationId = UUID.randomUUID()
    private val otherLocationId = UUID.randomUUID()
    private val sourceTypeCode = "SALE"
    private val accountCode = "001.001.001"

    // Organization zone +03:00: local day 8 March starts at 07 March 21:00 UTC.
    private val from = LocalDate.of(2026, 3, 8)
    private val before = LocalDate.of(2026, 3, 10)

    @BeforeEach
    fun createSchemaAndFixtures() {
        SessionContextProvider.setSession(SessionContext(organization = OrgSession(UUID.randomUUID(), "org-a", "+03:00")))
        connection = DriverManager.getConnection(postgres.jdbcUrl, postgres.username, postgres.password)
        connection.createStatement().use { statement ->
            statement.execute("DROP SCHEMA IF EXISTS ledger_movement_test CASCADE")
            statement.execute("CREATE SCHEMA ledger_movement_test")
            statement.execute("SET search_path TO ledger_movement_test")
            statement.execute(
                """CREATE TABLE ledger_entry_group (reference_number VARCHAR(30) PRIMARY KEY, source_type VARCHAR(10),
                   posted_on TIMESTAMP WITH TIME ZONE, source_location_id UUID)"""
            )
            statement.execute(
                """CREATE TABLE ledger_entry (id SERIAL PRIMARY KEY, group_reference_number VARCHAR(30), account_code VARCHAR(30),
                   entry_type VARCHAR(5), amount NUMERIC(19,4))"""
            )
        }
        insertPosting("BEFORE_RANGE", "2026-03-07T20:59:59Z", 1, locationId)
        insertPosting("DAY_ONE_START", "2026-03-07T21:00:00Z", 10, locationId)
        insertPosting("DAY_ONE_END", "2026-03-08T20:59:59Z", 20, otherLocationId)
        insertPosting("DAY_TWO_START", "2026-03-08T21:00:00Z", 100, locationId)
        insertPosting("RANGE_END_EXCLUDED", "2026-03-09T21:00:00Z", 1000, locationId)
    }

    @AfterEach
    fun closeConnection() {
        connection.close()
        SessionContextProvider.clear()
    }

    @Test
    fun `a posting lands in the local day it falls on and the range end is exclusive`() {
        val dayStarts = dayStarts()

        val rows = run(
            "sumMovementsInRange",
            mapOf(
                "dayStartsLiteral" to ledgerMovementReader.toDayStartsLiteral(dayStarts),
                "rangeStart" to dayStarts.first(),
                "rangeEnd" to dayStarts.last(),
                "sourceTypeCodes" to arrayOf(sourceTypeCode)
            )
        )

        assertEquals(mapOf(1 to BigDecimal(30), 2 to BigDecimal(100)), amountsByBucket(rows))
    }

    @Test
    fun `a location query keeps only that location's postings`() {
        val dayStarts = dayStarts()

        val rows = run(
            "sumMovementsInRangeForLocation",
            mapOf(
                "dayStartsLiteral" to ledgerMovementReader.toDayStartsLiteral(dayStarts),
                "rangeStart" to dayStarts.first(),
                "rangeEnd" to dayStarts.last(),
                "sourceTypeCodes" to arrayOf(sourceTypeCode),
                "locationId" to locationId
            )
        )

        assertEquals(mapOf(1 to BigDecimal(10), 2 to BigDecimal(100)), amountsByBucket(rows))
    }

    @Test
    fun `the open-ended account query buckets every posting from the range start`() {
        val dayStarts = dayStarts()

        val rows = run(
            "sumAccountMovementsFrom",
            mapOf(
                "dayStartsLiteral" to ledgerMovementReader.toDayStartsLiteral(dayStarts),
                "rangeStart" to dayStarts.first(),
                "accountCodes" to arrayOf(accountCode)
            )
        )

        assertEquals(mapOf(1 to BigDecimal(30), 2 to BigDecimal(100), 3 to BigDecimal(1000)), amountsByBucket(rows))
    }

    private fun dayStarts(): List<Instant> =
        generateSequence(from) { date -> date.plusDays(1) }
            .takeWhile { date -> !date.isAfter(before) }
            .map { date -> DateTimes.Offset.atStartOfDayInOrganizationZone(date).toInstant() }
            .toList()

    private fun amountsByBucket(rows: List<Map<String, Any?>>): Map<Int, BigDecimal> =
        rows.associate { row -> (row.getValue("bucket") as Int) to (row.getValue("amount") as BigDecimal).stripTrailingZeros() }
            .mapValues { (_, amount) -> amount.setScale(0) }

    private fun insertPosting(groupReferenceNumber: String, postedOn: String, amount: Int, sourceLocationId: UUID) {
        connection.prepareStatement("INSERT INTO ledger_entry_group VALUES (?, ?, ?, ?)").use { statement ->
            statement.setString(1, groupReferenceNumber)
            statement.setString(2, sourceTypeCode)
            statement.setObject(3, OffsetDateTime.ofInstant(Instant.parse(postedOn), ZoneOffset.UTC))
            statement.setObject(4, sourceLocationId)
            statement.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO ledger_entry (group_reference_number, account_code, entry_type, amount) VALUES (?, ?, 'D', ?)").use { statement ->
            statement.setString(1, groupReferenceNumber)
            statement.setString(2, accountCode)
            statement.setBigDecimal(3, BigDecimal(amount))
            statement.executeUpdate()
        }
    }

    private fun run(repositoryMethodName: String, parameters: Map<String, Any>): List<Map<String, Any?>> {
        val queryText = LedgerEntryRepository::class.java.declaredMethods
            .single { method -> method.name == repositoryMethodName }
            .getAnnotation(Query::class.java).value
        val parameterNames = mutableListOf<String>()
        val positionalSql = NAMED_PARAMETER.replace(queryText.replace(IN_LIST, "= ANY (:$1)")) { match ->
            parameterNames.add(match.groupValues[1])
            "?"
        }
        connection.prepareStatement(positionalSql).use { statement ->
            parameterNames.forEachIndexed { index, name -> bind(statement, index + 1, parameters.getValue(name)) }
            statement.executeQuery().use { resultSet ->
                val columnLabels = (1..resultSet.metaData.columnCount).map { resultSet.metaData.getColumnLabel(it) }
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
            is Array<*> -> statement.setArray(index, connection.createArrayOf("varchar", value))
            is Instant -> statement.setObject(index, OffsetDateTime.ofInstant(value, ZoneOffset.UTC))
            is String -> statement.setString(index, value)
            else -> statement.setObject(index, value)
        }
    }

    private companion object {
        private val postgres = PostgreSQLContainer<Nothing>("postgres:16")
        val NAMED_PARAMETER = Regex("(?<!:):([A-Za-z][A-Za-z0-9_]*)")
        val IN_LIST = Regex("IN \\(:([A-Za-z][A-Za-z0-9_]*)\\)")

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
    }
}
