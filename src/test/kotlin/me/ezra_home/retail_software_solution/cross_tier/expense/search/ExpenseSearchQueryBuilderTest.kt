package me.ezra_home.retail_software_solution.cross_tier.expense.search

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.queries.KeysetSearchCursor
import me.ezra_home.retail_software_solution.util.queries.QueryParameterNames
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

// Executes the SQL the builder actually emits against H2, so filter semantics are checked on rows
// rather than on the shape of the SQL text.
abstract class ExpenseSearchQueryBuilderContract {

    protected abstract val tables: ExpenseSearchTables

    private lateinit var connection: Connection

    private val typeA = UUID.randomUUID()
    private val typeB = UUID.randomUUID()
    private val payeeOne = UUID.randomUUID()
    private val payeeTwo = UUID.randomUUID()
    private val methodCash = UUID.randomUUID()
    private val methodCard = UUID.randomUUID()
    private val baseCreatedOn = OffsetDateTime.parse("2030-01-01T00:00:00Z")

    @BeforeEach
    fun createSchemaAndFixtures() {
        connection = DriverManager.getConnection("jdbc:h2:mem:${tables.expense}_search_query_builder_test;DB_CLOSE_DELAY=-1")
        connection.createStatement().use { statement ->
            statement.execute("DROP ALL OBJECTS")
            statement.execute(
                """CREATE TABLE ${tables.expense} (id UUID PRIMARY KEY, reference_number VARCHAR(30), expense_type_id UUID, payee_contact_id UUID,
                   amount NUMERIC(19,4), expense_date DATE, description VARCHAR(255), source_type VARCHAR(5), source_reference VARCHAR(50),
                   batch_id UUID, created_on TIMESTAMP WITH TIME ZONE, created_by_id UUID)"""
            )
            statement.execute(
                """CREATE TABLE ${tables.paymentState} (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, expense_id UUID UNIQUE,
                   payment_status VARCHAR(5), amount_paid NUMERIC(19,4))"""
            )
            statement.execute("CREATE TABLE ${tables.expenseVoid} (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, expense_id UUID UNIQUE)")
            statement.execute("CREATE TABLE ${tables.expensePayment} (id UUID PRIMARY KEY, expense_id UUID, payment_method_id UUID, amount NUMERIC(19,4))")
            statement.execute("CREATE TABLE ${tables.expensePaymentVoid} (id UUID DEFAULT RANDOM_UUID() PRIMARY KEY, expense_payment_id UUID UNIQUE)")
        }
        insertExpense("EXPN01", typeA, payeeOne, "100", "2030-01-01", "ADH", null, 1, PaymentStatus.UNPAID, "0")
        insertExpense("EXPN02", typeA, payeeOne, "200", "2030-01-02", "PUR", "PRCH01", 2, PaymentStatus.PARTIALLY_SETTLED, "50")
            .also { insertPayment(it, methodCash, "50", voided = false) }
        insertExpense("EXPN03", typeB, payeeTwo, "300", "2030-01-03", "SAL", "SALE01", 3, PaymentStatus.FULLY_SETTLED, "300")
            .also {
                insertPayment(it, methodCash, "100", voided = false)
                insertPayment(it, methodCard, "200", voided = false)
            }
        insertExpense("EXPN04", typeB, payeeTwo, "400", "2030-01-04", "ADH", null, 4, PaymentStatus.UNPAID, "0")
            .also {
                insertPayment(it, methodCard, "400", voided = true)
                voidExpense(it)
            }
        insertExpense("EXPN05", typeB, payeeOne, "50", "2030-01-05", "WAG", "WAGE01", 5, PaymentStatus.UNPAID, "0")
            .also { insertPayment(it, methodCard, "50", voided = true) }
    }

    @AfterEach
    fun closeConnection() {
        connection.close()
    }

    @Test
    fun `an empty search lists every expense newest first`() {
        val rows = list(ExpenseSearchParameters())

        assertEquals(listOf("EXPN05", "EXPN04", "EXPN03", "EXPN02", "EXPN01"), rows.map { it["reference_number"] })
    }

    @Test
    fun `created range includes the from instant and excludes the before instant`() {
        val rows = list(ExpenseSearchParameters(createdFrom = baseCreatedOn.plusHours(2), createdBefore = baseCreatedOn.plusHours(4)))

        assertEquals(listOf("EXPN03", "EXPN02"), references(rows))
    }

    @Test
    fun `expense date range includes the from date and excludes the before date`() {
        val rows = list(ExpenseSearchParameters(expenseDateFrom = LocalDate.of(2030, 1, 2), expenseDateBefore = LocalDate.of(2030, 1, 4)))

        assertEquals(listOf("EXPN03", "EXPN02"), references(rows))
    }

    @Test
    fun `payee and expense type filters match any listed id`() {
        assertEquals(listOf("EXPN05", "EXPN02", "EXPN01"), references(list(ExpenseSearchParameters(payeeContactIds = listOf(payeeOne)))))
        assertEquals(
            listOf("EXPN05", "EXPN04", "EXPN03"),
            references(list(ExpenseSearchParameters(expenseTypeIds = listOf(typeB, UUID.randomUUID()))))
        )
    }

    @Test
    fun `source references match on the reference alone with no source type supplied`() {
        val rows = list(ExpenseSearchParameters(sourceReferences = listOf("PRCH01", "WAGE01")))

        assertEquals(listOf("EXPN05", "EXPN02"), references(rows))
    }

    @Test
    fun `expense reference numbers match any listed reference`() {
        val rows = list(ExpenseSearchParameters(expenseReferenceNumbers = listOf("EXPN01", "EXPN04")))

        assertEquals(listOf("EXPN04", "EXPN01"), references(rows))
    }

    @Test
    fun `amount range is inclusive at both ends`() {
        val rows = list(ExpenseSearchParameters(minAmount = BigDecimal("200"), maxAmount = BigDecimal("300")))

        assertEquals(listOf("EXPN03", "EXPN02"), references(rows))
    }

    @Test
    fun `payment status filters on the stored state across pay, partial pay, payment void and expense void`() {
        assertEquals(
            listOf("EXPN05", "EXPN04", "EXPN01"),
            references(list(ExpenseSearchParameters(paymentStatuses = setOf(PaymentStatus.UNPAID))))
        )
        assertEquals(listOf("EXPN02"), references(list(ExpenseSearchParameters(paymentStatuses = setOf(PaymentStatus.PARTIALLY_SETTLED)))))
        assertEquals(listOf("EXPN03"), references(list(ExpenseSearchParameters(paymentStatuses = setOf(PaymentStatus.FULLY_SETTLED)))))
        assertEquals(
            listOf("EXPN03", "EXPN02"),
            references(list(ExpenseSearchParameters(paymentStatuses = setOf(PaymentStatus.PARTIALLY_SETTLED, PaymentStatus.FULLY_SETTLED))))
        )
    }

    @Test
    fun `voided is a tri-state flag independent of payment status`() {
        assertEquals(5, list(ExpenseSearchParameters(voided = null)).size)
        assertEquals(listOf("EXPN04"), references(list(ExpenseSearchParameters(voided = true))))
        assertEquals(listOf("EXPN05", "EXPN03", "EXPN02", "EXPN01"), references(list(ExpenseSearchParameters(voided = false))))
    }

    @Test
    fun `payment method filter ignores voided payments and never duplicates an expense`() {
        assertEquals(
            listOf("EXPN03"),
            references(list(ExpenseSearchParameters(paymentMethodIds = listOf(methodCard)))),
            "EXPN04 and EXPN05 only hold voided card payments"
        )
        assertEquals(
            listOf("EXPN03", "EXPN02"),
            references(list(ExpenseSearchParameters(paymentMethodIds = listOf(methodCash, methodCard)))),
            "EXPN03 matches through two payments and must appear once"
        )
    }

    @Test
    fun `filters combine with and`() {
        val rows = list(
            ExpenseSearchParameters(
                expenseTypeIds = listOf(typeB),
                paymentStatuses = setOf(PaymentStatus.UNPAID),
                voided = false,
                payeeContactIds = listOf(payeeOne)
            )
        )

        assertEquals(listOf("EXPN05"), references(rows))
    }

    @Test
    fun `keyset paging walks every row once across the page boundary`() {
        val parameters = ExpenseSearchParameters()
        val firstPage = list(parameters, pageSize = 2)
        val secondPage = list(parameters, cursorAfter(firstPage.last()), pageSize = 2)
        val thirdPage = list(parameters, cursorAfter(secondPage.last()), pageSize = 2)

        assertEquals(listOf("EXPN05", "EXPN04"), references(firstPage))
        assertEquals(listOf("EXPN03", "EXPN02"), references(secondPage))
        assertEquals(listOf("EXPN01"), references(thirdPage))
    }

    @Test
    fun `rows sharing a created instant are split across pages by id without loss or repeats`() {
        val sharedInstant = baseCreatedOn.plusHours(10)
        val tiedIds = (1..3).map { insertExpense("EXPT0$it", typeA, payeeOne, "10", "2030-02-01", "ADH", null, 10, PaymentStatus.UNPAID, "0") }
        val parameters = ExpenseSearchParameters(createdFrom = sharedInstant, createdBefore = sharedInstant.plusSeconds(1))

        val firstPage = list(parameters, pageSize = 2)
        val secondPage = list(parameters, cursorAfter(firstPage.last()), pageSize = 2)

        assertEquals(tiedIds.sortedWith(UNSIGNED_UUID_ORDER).reversed(), (firstPage + secondPage).map { it["id"] as UUID })
    }

    @Test
    fun `summary groups by type, voided flag and stored status with paid and outstanding zeroed for voided`() {
        val summaryRows = summarize(ExpenseSearchParameters(expenseTypeIds = listOf(typeA, typeB)))
            .associateBy { Triple(it["expense_type_id"], it["voided"], it["payment_status"]) }

        fun row(typeId: UUID, voided: Boolean, status: PaymentStatus) = summaryRows.getValue(Triple(typeId, voided, status.code))

        assertEquals(5, summaryRows.size)
        assertSummary(row(typeA, false, PaymentStatus.UNPAID), count = 1, amount = "100", paid = "0", outstanding = "100")
        assertSummary(row(typeA, false, PaymentStatus.PARTIALLY_SETTLED), count = 1, amount = "200", paid = "50", outstanding = "150")
        assertSummary(row(typeB, false, PaymentStatus.FULLY_SETTLED), count = 1, amount = "300", paid = "300", outstanding = "0")
        assertSummary(row(typeB, false, PaymentStatus.UNPAID), count = 1, amount = "50", paid = "0", outstanding = "50")
        assertSummary(row(typeB, true, PaymentStatus.UNPAID), count = 1, amount = "400", paid = "0", outstanding = "0")
    }

    @Test
    fun `summary of a voided expense never reports paid money even if its state row says otherwise`() {
        connection.createStatement().use {
            it.execute("UPDATE ${tables.paymentState} SET payment_status = 'FST', amount_paid = 400 WHERE expense_id IN (SELECT expense_id FROM ${tables.expenseVoid})")
        }

        val voidedRow = summarize(ExpenseSearchParameters(voided = true)).single()

        assertSummary(voidedRow, count = 1, amount = "400", paid = "0", outstanding = "0")
    }

    @Test
    fun `summary honours the same filters as the list`() {
        val summaryRows = summarize(ExpenseSearchParameters(paymentMethodIds = listOf(methodCash)))

        assertEquals(2, summaryRows.sumOf { (it["expense_count"] as Number).toInt() })
    }

    @Test
    fun `the list query pages with a keyset on created_on and id and joins payments only through exists`() {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(ExpenseSearchParameters(paymentMethodIds = listOf(methodCash)), tables)
        val cursor = KeysetSearchCursor(baseCreatedOn, UUID.randomUUID())

        val sql = ExpenseSearchQueryBuilder.buildListQuery(predicate, cursor, tables).sql

        assertTrue(sql.contains("(e.created_on, e.id) < (:${QueryParameterNames.CURSOR_CREATED_ON}, :${QueryParameterNames.CURSOR_ID})"))
        assertTrue(sql.contains("ORDER BY e.created_on DESC, e.id DESC"))
        assertTrue(sql.contains("EXISTS ("))
        assertFalse(sql.contains("JOIN ${tables.expensePayment} "))
        assertFalse(
            ExpenseSearchQueryBuilder.buildListQuery(ExpenseSearchQueryBuilder.buildPredicate(ExpenseSearchParameters(), tables), null, tables)
                .sql.contains("cursor")
        )
    }

    private fun assertSummary(row: Map<String, Any?>, count: Int, amount: String, paid: String, outstanding: String) {
        assertEquals(count, (row["expense_count"] as Number).toInt())
        assertEquals(0, BigDecimal(amount).compareTo(row["amount_total"] as BigDecimal), "amount")
        assertEquals(0, BigDecimal(paid).compareTo(row["paid_total"] as BigDecimal), "paid")
        assertEquals(0, BigDecimal(outstanding).compareTo(row["outstanding_total"] as BigDecimal), "outstanding")
    }

    private fun references(rows: List<Map<String, Any?>>) = rows.map { it["reference_number"] }

    private fun cursorAfter(row: Map<String, Any?>) =
        KeysetSearchCursor((row["created_on"] as OffsetDateTime), row["id"] as UUID)

    private fun list(
        expenseSearchParameters: ExpenseSearchParameters,
        cursor: KeysetSearchCursor? = null,
        pageSize: Int = 50
    ): List<Map<String, Any?>> {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(expenseSearchParameters, tables)
        return execute(ExpenseSearchQueryBuilder.buildListQuery(predicate, cursor, tables), mapOf(QueryParameterNames.PAGE_SIZE to pageSize))
    }

    private fun summarize(expenseSearchParameters: ExpenseSearchParameters): List<Map<String, Any?>> {
        val predicate = ExpenseSearchQueryBuilder.buildPredicate(expenseSearchParameters, tables)
        return execute(ExpenseSearchQueryBuilder.buildSummaryQuery(predicate, tables), emptyMap())
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
            is Array<*> -> statement.setArray(index, connection.createArrayOf(if (value.firstOrNull() is UUID) "UUID" else "VARCHAR", value))
            else -> statement.setObject(index, value)
        }
    }

    private fun insertExpense(
        referenceNumber: String,
        expenseTypeId: UUID,
        payeeContactId: UUID,
        amount: String,
        expenseDate: String,
        sourceType: String,
        sourceReference: String?,
        createdHoursAfterBase: Long,
        paymentStatus: PaymentStatus,
        amountPaid: String
    ): UUID {
        val expenseId = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO ${tables.expense} VALUES (?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?, ?)").use {
            it.setObject(1, expenseId)
            it.setString(2, referenceNumber)
            it.setObject(3, expenseTypeId)
            it.setObject(4, payeeContactId)
            it.setBigDecimal(5, BigDecimal(amount))
            it.setObject(6, LocalDate.parse(expenseDate))
            it.setString(7, sourceType)
            it.setString(8, sourceReference)
            it.setObject(9, UUID.randomUUID())
            it.setObject(10, baseCreatedOn.plusHours(createdHoursAfterBase))
            it.setObject(11, UUID.randomUUID())
            it.executeUpdate()
        }
        connection.prepareStatement("INSERT INTO ${tables.paymentState} (expense_id, payment_status, amount_paid) VALUES (?, ?, ?)").use {
            it.setObject(1, expenseId)
            it.setString(2, paymentStatus.code)
            it.setBigDecimal(3, BigDecimal(amountPaid))
            it.executeUpdate()
        }
        return expenseId
    }

    private fun insertPayment(expenseId: UUID, paymentMethodId: UUID, amount: String, voided: Boolean) {
        val paymentId = UUID.randomUUID()
        connection.prepareStatement("INSERT INTO ${tables.expensePayment} VALUES (?, ?, ?, ?)").use {
            it.setObject(1, paymentId)
            it.setObject(2, expenseId)
            it.setObject(3, paymentMethodId)
            it.setBigDecimal(4, BigDecimal(amount))
            it.executeUpdate()
        }
        if (voided) {
            connection.prepareStatement("INSERT INTO ${tables.expensePaymentVoid} (expense_payment_id) VALUES (?)").use {
                it.setObject(1, paymentId)
                it.executeUpdate()
            }
        }
    }

    private fun voidExpense(expenseId: UUID) {
        connection.prepareStatement("INSERT INTO ${tables.expenseVoid} (expense_id) VALUES (?)").use {
            it.setObject(1, expenseId)
            it.executeUpdate()
        }
    }

    companion object {
        private val NAMED_PARAMETER = Regex("(?<!:):([A-Za-z][A-Za-z0-9_]*)")

        // Postgres orders uuid as unsigned bytes; java.util.UUID.compareTo is signed and disagrees.
        private val UNSIGNED_UUID_ORDER = compareBy<UUID>({ it.mostSignificantBits.toULong() }, { it.leastSignificantBits.toULong() })
    }
}

class LocationExpenseSearchQueryBuilderTest : ExpenseSearchQueryBuilderContract() {
    override val tables = ExpenseSearchTables.LOCATION
}

class OrganizationExpenseSearchQueryBuilderTest : ExpenseSearchQueryBuilderContract() {
    override val tables = ExpenseSearchTables.ORGANIZATION
}
