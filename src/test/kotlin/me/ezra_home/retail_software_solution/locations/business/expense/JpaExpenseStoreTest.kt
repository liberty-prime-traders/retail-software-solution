package me.ezra_home.retail_software_solution.locations.business.expense

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseBatchBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpensePaymentVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.entities.ExpenseVoidBase
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpensePaymentDraft
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ExpenseRecord
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedExpenseRow
import me.ezra_home.retail_software_solution.cross_tier.expense.record.ResolvedSettlement
import me.ezra_home.retail_software_solution.cross_tier.expense.store.JpaExpenseStore
import me.ezra_home.retail_software_solution.messaging.kafka.common.EventSourceContext
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyCollection
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

class JpaExpenseStoreTest {

    private val expenseBatchRepository = mock(ExpenseBatchRepository::class.java)
    private val expenseRepository = mock(ExpenseRepository::class.java)
    private val expensePaymentRepository = mock(ExpensePaymentRepository::class.java)
    private val expensePaymentVoidRepository = mock(ExpensePaymentVoidRepository::class.java)
    private val expenseVoidRepository = mock(ExpenseVoidRepository::class.java)
    private val expensePaymentStateRepository = mock(ExpensePaymentStateRepository::class.java)

    private val expenseStore = object : JpaExpenseStore(
        expenseBatchRepository, expenseRepository, expensePaymentRepository, expensePaymentVoidRepository, expenseVoidRepository,
        expensePaymentStateRepository
    ) {
        override fun saveNewBatch(description: String, sourceType: ExpenseSourceType, sourceReference: String?): ExpenseBatchBase =
            persisted(ExpenseBatchEntity(description, sourceType, sourceReference))

        override fun saveNewExpense(
            batchId: UUID,
            sourceType: ExpenseSourceType,
            sourceReference: String?,
            resolvedExpenseRow: ResolvedExpenseRow
        ): ExpenseBase = persisted(expenseEntity(batchId))

        override fun saveNewPayments(expensePaymentDrafts: List<ExpensePaymentDraft>): List<ExpensePaymentBase> =
            expensePaymentDrafts.map { persisted(paymentEntity(it.expenseId, it.amount)) }

        override fun saveNewPaymentVoid(paymentId: UUID, reason: String): ExpensePaymentVoidBase =
            persisted(ExpensePaymentVoidEntity(paymentId, reason))

        override fun saveNewExpenseVoid(expenseId: UUID, reason: String): ExpenseVoidBase =
            persisted(ExpenseVoidEntity(expenseId, reason))

        override fun sourceContext() = EventSourceContext.OrgLevel(orgSchema = "org-a")

        override fun lockExpense(expenseId: UUID) = Unit

        override fun refreshPaymentStates(expenseRecords: Collection<ExpenseRecord>) = Unit

        override fun lockSourceDocument(sourceDocumentId: UUID) = Unit
    }

    private val batchEntity = persisted(ExpenseBatchEntity("Wages", ExpenseSourceType.WAGES, null))
    private val expenseEntity = persisted(expenseEntity(batchEntity.id!!))
    private val paymentEntity = persisted(paymentEntity(expenseEntity.id!!, BigDecimal("40.0000")))

    @Test
    fun `a payment record carries the account code copied when it was recorded`() {
        `when`(expensePaymentRepository.findByReferenceNumber("EXPY01")).thenReturn(paymentEntity)

        val expensePaymentRecord = expenseStore.findPaymentByReference("EXPY01")!!

        assertEquals("001.099", expensePaymentRecord.paymentMethodAccountCode)
        assertEquals(paymentEntity.paymentMethodId, expensePaymentRecord.paymentMethodId)
        assertEquals(expenseEntity.id, expensePaymentRecord.expenseId)
    }

    @Test
    fun `an expense record carries the expense account code copied when it was recorded`() {
        `when`(expenseRepository.findById(expenseEntity.id!!)).thenReturn(Optional.of(expenseEntity))

        val expenseRecord = expenseStore.findExpenseById(expenseEntity.id!!)!!

        assertEquals("005.005", expenseRecord.expenseAccountCode)
        assertEquals(batchEntity.id, expenseRecord.batchId)
    }

    @Test
    fun `a lookup for an unknown reference returns null`() {
        assertNull(expenseStore.findExpenseByReference("EXPN99"))
        assertNull(expenseStore.findPaymentById(UUID.randomUUID()))
    }

    @Test
    fun `loading expenses assembles their batches, payments and both kinds of void`() {
        val expenseId = expenseEntity.id!!
        val paymentVoidEntity = persisted(ExpensePaymentVoidEntity(paymentEntity.id!!, "wrong method"))
        val expenseVoidEntity = persisted(ExpenseVoidEntity(expenseId, "duplicate"))
        `when`(expenseRepository.findAllById(listOf(expenseId))).thenReturn(listOf(expenseEntity))
        `when`(expenseBatchRepository.findAllById(setOf(batchEntity.id!!))).thenReturn(listOf(batchEntity))
        `when`(expensePaymentRepository.findByExpenseIdIn(listOf(expenseId))).thenReturn(listOf(paymentEntity))
        `when`(expensePaymentVoidRepository.findByExpensePaymentIdIn(listOf(paymentEntity.id!!))).thenReturn(listOf(paymentVoidEntity))
        `when`(expenseVoidRepository.findByExpenseIdIn(listOf(expenseId))).thenReturn(listOf(expenseVoidEntity))

        val expenseAggregate = expenseStore.loadForExpenses(listOf(expenseId))

        assertEquals(listOf(batchEntity.id), expenseAggregate.batches.map { it.id })
        assertEquals(listOf(expenseId), expenseAggregate.expenses.map { it.id })
        assertEquals(listOf(paymentEntity.id), expenseAggregate.payments.map { it.id })
        assertEquals(listOf(paymentEntity.id), expenseAggregate.paymentVoids.map { it.paymentId })
        assertEquals(listOf(expenseId), expenseAggregate.expenseVoids.map { it.expenseId })
    }

    @Test
    fun `loading an expense id that does not exist fails instead of returning a partial aggregate`() {
        val missingExpenseId = UUID.randomUUID()
        `when`(expenseRepository.findAllById(listOf(missingExpenseId))).thenReturn(emptyList())

        assertThrows(RtsGenericException::class.java) { expenseStore.loadForExpenses(listOf(missingExpenseId)) }
    }

    @Test
    fun `loading a batch that does not exist fails`() {
        val missingBatchId = UUID.randomUUID()
        `when`(expenseBatchRepository.findById(missingBatchId)).thenReturn(Optional.empty())

        assertThrows(RtsGenericException::class.java) { expenseStore.loadForBatch(missingBatchId) }
    }

    @Test
    fun `a batch with no expenses skips the payment and void queries`() {
        `when`(expenseBatchRepository.findById(batchEntity.id!!)).thenReturn(Optional.of(batchEntity))
        `when`(expenseRepository.findByBatchId(batchEntity.id!!)).thenReturn(emptyList())

        val expenseAggregate = expenseStore.loadForBatch(batchEntity.id!!)

        assertEquals(1, expenseAggregate.batches.size)
        assertTrue(expenseAggregate.expenses.isEmpty())
        verify(expensePaymentRepository, never()).findByExpenseIdIn(anyCollection())
        verify(expensePaymentVoidRepository, never()).findByExpensePaymentIdIn(anyCollection())
        verify(expenseVoidRepository, never()).findByExpenseIdIn(anyCollection())
    }

    @Test
    fun `an expense with no payments skips the payment void query`() {
        val expenseId = expenseEntity.id!!
        `when`(expenseRepository.findAllById(listOf(expenseId))).thenReturn(listOf(expenseEntity))
        `when`(expenseBatchRepository.findAllById(setOf(batchEntity.id!!))).thenReturn(listOf(batchEntity))
        `when`(expensePaymentRepository.findByExpenseIdIn(listOf(expenseId))).thenReturn(emptyList())
        `when`(expenseVoidRepository.findByExpenseIdIn(listOf(expenseId))).thenReturn(emptyList())

        expenseStore.loadForExpenses(listOf(expenseId))

        verify(expensePaymentVoidRepository, never()).findByExpensePaymentIdIn(anyCollection())
    }

    @Test
    fun `recent expenses are the newest first, with the id breaking ties`() {
        `when`(expenseRepository.findAll(any(Pageable::class.java))).thenReturn(PageImpl(listOf(expenseEntity)))
        `when`(expenseBatchRepository.findAllById(setOf(batchEntity.id!!))).thenReturn(listOf(batchEntity))

        expenseStore.loadRecent(25)

        val pageRequestCaptor = ArgumentCaptor.forClass(Pageable::class.java)
        verify(expenseRepository).findAll(pageRequestCaptor.capture())
        assertEquals(PageRequest.of(0, 25, Sort.by(Sort.Direction.DESC, "createdOn", "id")), pageRequestCaptor.value)
    }

    @Test
    fun `saved payments come back as records in the order they were drafted`() {
        val expenseId = expenseEntity.id!!
        val settlement = ResolvedSettlement(UUID.randomUUID(), "001.099", null, LocalDate.of(2026, 3, 10))
        val drafts = listOf("10", "20", "30").map { ExpensePaymentDraft(expenseId, BigDecimal(it), settlement) }

        val expensePaymentRecords = expenseStore.savePayments(drafts)

        assertEquals(listOf("10", "20", "30"), expensePaymentRecords.map { it.amount.toPlainString() })
    }

    private fun expenseEntity(batchId: UUID) = ExpenseEntity(
        expenseTypeId = UUID.randomUUID(),
        expenseAccountCode = "005.005",
        payeeContactId = UUID.randomUUID(),
        amount = BigDecimal("100.0000"),
        expenseDate = LocalDate.of(2026, 3, 10),
        description = null,
        sourceType = ExpenseSourceType.WAGES,
        sourceReference = null,
        batchId = batchId
    )

    private fun paymentEntity(expenseId: UUID, amount: BigDecimal) = ExpensePaymentEntity(
        expenseId = expenseId,
        paymentMethodId = UUID.randomUUID(),
        paymentMethodAccountCode = "001.099",
        amount = amount,
        providerReference = null,
        paymentDate = OffsetDateTime.now()
    )

    private fun <T : ImmutableEntity> persisted(entity: T): T = entity.apply {
        id = UUID.randomUUID()
        createdOn = OffsetDateTime.now()
        createdById = UUID.randomUUID()
        referenceNumber = "REF-${id.toString().take(6)}"
    }
}
