package me.ezra_home.retail_software_solution.locations.business.purchase

import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentSearchDto
import me.ezra_home.retail_software_solution.locations.business.purchase.api.bulk_payment.PurchaseBulkPaymentService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService
import me.ezra_home.retail_software_solution.locations.business.purchase.api.payment.PurchasePaymentCeilingService.PaymentCeiling
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseStatus
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentCreateDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentDataFetcher
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentResponseDto
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentService
import me.ezra_home.retail_software_solution.util.business.PositiveMoney
import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class PurchaseBulkPaymentServiceTest {

  private val supplierId = UUID.randomUUID()
  private val paymentMethodId = UUID.randomUUID()
  private val paymentDate = OffsetDateTime.of(2026, 3, 1, 10, 0, 0, 0, ZoneOffset.UTC)
  private val january = OffsetDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

  private val recordedCreateDtos = mutableListOf<SupplierPaymentCreateDto>()
  private val purchaseDataFetcher = mock(PurchaseDataFetcher::class.java)
  private val purchaseRepository = mock(PurchaseRepository::class.java)
  private val payablePurchaseFinder = mock(PayablePurchaseFinder::class.java)
  private val paidAmountsByPurchaseId = mutableMapOf<UUID, BigDecimal>()
  private val ceilingsByPurchaseId = mutableMapOf<UUID, PaymentCeiling>()
  private val purchasePaymentCeilingService = mock(PurchasePaymentCeilingService::class.java)
  private val supplierPaymentDataFetcher = mock(SupplierPaymentDataFetcher::class.java)
  private val supplierPaymentService = mock(SupplierPaymentService::class.java)

  private val purchaseBulkPaymentService = PurchaseBulkPaymentService(
    purchaseDataFetcher,
    payablePurchaseFinder,
    PurchaseBulkPaymentPlanner(purchaseRepository, purchasePaymentCeilingService, supplierPaymentDataFetcher),
    supplierPaymentService,
    supplierPaymentDataFetcher,
    purchasePaymentCeilingService
  )

  @Suppress("UNCHECKED_CAST")
  private fun <T> anyNonNull(): T {
    ArgumentMatchers.any<T>()
    return null as T
  }

  private fun stubPurchase(
    dateOrdered: OffsetDateTime,
    createdOn: OffsetDateTime = dateOrdered,
    ownerSupplierId: UUID = supplierId,
    ceilingAmount: String = "100.00",
    alreadyPaid: String = "0.00",
    purchaseStatus: PurchaseStatus = PurchaseStatus.FULLY_DELIVERED,
    paymentStatus: PaymentStatus = PaymentStatus.UNPAID
  ): PurchaseEntity {
    val purchaseId = UUID.randomUUID()
    paidAmountsByPurchaseId[purchaseId] = BigDecimal(alreadyPaid)
    ceilingsByPurchaseId[purchaseId] = PaymentCeiling(
      BigDecimal(ceilingAmount),
      purchaseStatus == PurchaseStatus.FULLY_DELIVERED,
      BigDecimal(ceilingAmount),
      BigDecimal(ceilingAmount)
    )
    return PurchaseEntity(
      supplierId = ownerSupplierId, purchaseStatus = purchaseStatus, paymentStatus = paymentStatus,
      dateOrdered = dateOrdered
    ).also {
      it.id = purchaseId
      it.referenceNumber = "PRCH-$purchaseId"
      it.createdOn = createdOn
    }
  }

  private fun givenPaidAmountsLookup() {
    doAnswer { invocation ->
      invocation.getArgument<Collection<UUID>>(0).associateWith { ceilingsByPurchaseId.getValue(it) }
    }.`when`(purchasePaymentCeilingService).computeCeilings(anyNonNull())
    doAnswer { invocation ->
      invocation.getArgument<Collection<UUID>>(0).associateWith { paidAmountsByPurchaseId.getValue(it) }
    }.`when`(supplierPaymentDataFetcher).getPaidAmountsByPurchaseIds(anyNonNull())
  }

  private fun givenFetched(vararg purchases: PurchaseEntity) {
    givenPaidAmountsLookup()
    `when`(purchaseRepository.findAllById(anyNonNull<Iterable<UUID>>())).thenReturn(purchases.toList())
    doAnswer { invocation ->
      invocation.getArgument<List<SupplierPaymentCreateDto>>(0).map { createDto ->
        recordedCreateDtos.add(createDto)
        SupplierPaymentResponseDto(
          id = UUID.randomUUID(), referenceNumber = "SUP-${createDto.purchaseId}", purchaseId = createDto.purchaseId,
          purchaseReferenceNumber = "", paymentMethod = "", supplier = "", amount = createDto.amount.toBigDecimal(),
          paymentDate = createDto.paymentDate, notes = createDto.notes, createdBy = "", createdOn = ""
        )
      }
    }.`when`(supplierPaymentService)
      .recordPaymentsForLockedPurchases(anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull())
  }

  private fun record(
    purchases: List<PurchaseEntity>,
    amount: String,
    selectedPurchaseIds: Set<UUID> = purchases.map { it.id!! }.toSet()
  ) = purchaseBulkPaymentService.record(
    PurchaseBulkPaymentCreateDto(
      supplierId = supplierId,
      purchaseIds = selectedPurchaseIds,
      amount = PositiveMoney.of(BigDecimal(amount)),
      paymentMethodId = paymentMethodId,
      paymentDate = paymentDate,
      notes = "bulk"
    )
  )

  private fun verifyNothingRecorded() = verify(supplierPaymentService, never())
    .recordPaymentsForLockedPurchases(anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull())

  private fun searchDto() = PurchaseBulkPaymentSearchDto(supplierId, january, january.plusMonths(1))

  private fun appliedPurchaseIds(purchaseBulkPaymentResponseDtos: List<PurchaseBulkPaymentResponseDto>) =
    purchaseBulkPaymentResponseDtos.filter { it.appliedAmount.signum() > 0 }.map { it.id }

  @Test
  fun `allocates the earliest recorded purchase first, in the order the load lists them`() {
    val newest = stubPurchase(january, createdOn = january.plusMonths(1))
    val tiedA = stubPurchase(january, createdOn = january.plusHours(2))
    val tiedB = stubPurchase(january, createdOn = january.plusHours(2))
    givenFetched(newest, tiedA, tiedB)
    val expectedOrder = listOf(tiedA.id!!, tiedB.id!!).sortedBy { it.toString() } + newest.id!!

    val response = record(listOf(newest, tiedA, tiedB), "300.00")

    assertEquals(expectedOrder, appliedPurchaseIds(response))
    assertEquals(expectedOrder, recordedCreateDtos.map { it.purchaseId })
  }

  @Test
  fun `last purchase reached gets the remainder and later purchases get no payment`() {
    val first = stubPurchase(january)
    val second = stubPurchase(january.plusDays(1))
    val third = stubPurchase(january.plusDays(2))
    givenFetched(first, second, third)

    val response = record(listOf(first, second, third), "130.00")

    assertEquals(listOf(first.id!!, second.id!!), appliedPurchaseIds(response))
    assertEquals(
      listOf(BigDecimal("100.00"), BigDecimal("30.00")),
      response.filter { it.appliedAmount.signum() > 0 }.map { it.appliedAmount }
    )
    assertEquals(BigDecimal("130.00"), response.sumOf { it.appliedAmount })
    assertEquals(2, recordedCreateDtos.size)
  }

  @Test
  fun `caps each purchase at its current ceiling whatever its delivery state`() {
    val partiallyDelivered = stubPurchase(
      january, ceilingAmount = "80.00", purchaseStatus = PurchaseStatus.PARTIALLY_DELIVERED
    )
    val delivered = stubPurchase(january.plusDays(1))
    givenFetched(partiallyDelivered, delivered)

    val response = record(listOf(partiallyDelivered, delivered), "150.00")

    assertEquals(
      listOf(BigDecimal("80.00"), BigDecimal("70.00")),
      response.map { it.appliedAmount }
    )
    assertThrows<RtsGenericException> { record(listOf(partiallyDelivered, delivered), "180.01") }
  }

  @Test
  fun `rejects when amount exceeds total payable and states the total`() {
    val first = stubPurchase(january, alreadyPaid = "40.00")
    val second = stubPurchase(january.plusDays(1))
    givenFetched(first, second)

    val exception = assertThrows<RtsGenericException> { record(listOf(first, second), "160.01") }

    assertTrue(exception.message.contains("160.00"))
    verifyNothingRecorded()
  }

  @Test
  fun `rejects a purchase that belongs to a different supplier`() {
    val own = stubPurchase(january)
    val foreign = stubPurchase(january, ownerSupplierId = UUID.randomUUID())
    givenFetched(own, foreign)

    val exception = assertThrows<RtsGenericException> { record(listOf(own, foreign), "50.00") }

    assertTrue(exception.message.contains(foreign.id!!.toString()))
    verifyNothingRecorded()
  }

  @Test
  fun `rejects a purchase that does not exist`() {
    val existing = stubPurchase(january)
    val missingPurchaseId = UUID.randomUUID()
    givenFetched(existing)

    val exception = assertThrows<RtsGenericException> {
      record(listOf(existing), "50.00", selectedPurchaseIds = setOf(existing.id!!, missingPurchaseId))
    }

    assertTrue(exception.message.contains(missingPurchaseId.toString()))
  }

  @Test
  fun `rejects draft or canceled purchases`() {
    val draft = stubPurchase(january, purchaseStatus = PurchaseStatus.DRAFT)
    givenFetched(draft)

    val exception = assertThrows<RtsGenericException> { record(listOf(draft), "10.00") }

    assertTrue(exception.message.contains(draft.id!!.toString()))
    verifyNothingRecorded()
  }

  @Test
  fun `locks the selected purchases in ascending id order before reading any balance`() {
    val purchases = listOf(stubPurchase(january), stubPurchase(january.plusDays(1)), stubPurchase(january.plusDays(2)))
    givenFetched(*purchases.toTypedArray())

    record(purchases.reversed(), "10.00")

    val lockThenRead = inOrder(purchaseDataFetcher, supplierPaymentDataFetcher)
    purchases.map { it.id!! }.sorted().forEach { lockThenRead.verify(purchaseDataFetcher).lockPurchase(it) }
    lockThenRead.verify(supplierPaymentDataFetcher).getPaidAmountsByPurchaseIds(anyNonNull())
  }

  @Test
  fun `a purchase with nothing payable is not paid and shows as such`() {
    val settled = stubPurchase(january, alreadyPaid = "100.00")
    val open = stubPurchase(january.plusDays(1))
    givenFetched(settled, open)

    val response = record(listOf(settled, open), "60.00")

    val settledLine = response.single { it.id == settled.id!! }
    assertEquals(0, settledLine.payable.signum())
    assertEquals(0, settledLine.appliedAmount.signum())
    assertEquals(listOf(open.id!!), appliedPurchaseIds(response))
  }

  @Test
  fun `failure while recording the batch propagates so the surrounding transaction rolls back`() {
    val first = stubPurchase(january)
    val second = stubPurchase(january.plusDays(1))
    givenFetched(first, second)
    doAnswer { throw IllegalStateException("boom") }.`when`(supplierPaymentService)
      .recordPaymentsForLockedPurchases(anyNonNull(), anyNonNull(), anyNonNull(), anyNonNull())

    assertThrows<IllegalStateException> { record(listOf(first, second), "150.00") }
  }

  @Test
  fun `load keeps the order the search returned and computes payable from the current ceiling`() {
    val newer = stubPurchase(
      january.plusDays(1), alreadyPaid = "25.00", paymentStatus = PaymentStatus.PARTIALLY_SETTLED
    )
    val older = stubPurchase(january, ceilingAmount = "80.00", purchaseStatus = PurchaseStatus.ORDERED)
    `when`(payablePurchaseFinder.findPayablePurchases(anyNonNull()))
      .thenReturn(listOf(newer, older))
    givenPaidAmountsLookup()

    val response = purchaseBulkPaymentService.loadPayable(searchDto())

    assertEquals(listOf(newer.id!!, older.id!!), response.map { it.id })
    assertEquals(listOf(BigDecimal("75.00"), BigDecimal("80.00")), response.map { it.payable })
  }

  @Test
  fun `rejects more than twenty purchases in one batch`() {
    val purchases = (1..21).map { stubPurchase(january.plusDays(it.toLong())) }
    givenFetched(*purchases.toTypedArray())

    val exception = assertThrows<RtsGenericException> { record(purchases, "10.00") }

    assertTrue(exception.message.contains("20"))
    verifyNothingRecorded()
  }

  @Test
  fun `load asks the fetcher for the supplier's payable purchases in the given range`() {
    val purchase = stubPurchase(january)
    val searchedSuppliers = mutableListOf<UUID>()
    doAnswer { invocation ->
      searchedSuppliers.add(invocation.getArgument<PurchaseBulkPaymentSearchDto>(0).supplierId)
      listOf(purchase)
    }.`when`(payablePurchaseFinder).findPayablePurchases(anyNonNull())
    givenPaidAmountsLookup()

    val response = purchaseBulkPaymentService.loadPayable(searchDto())

    assertEquals(listOf(purchase.id), response.map { it.id })
    assertEquals(listOf(supplierId), searchedSuppliers)
    assertEquals(20, PurchaseBulkPaymentService.MAX_PAID_PURCHASES)
  }

  @Test
  fun `load of a supplier with nothing payable reads no balances`() {
    `when`(payablePurchaseFinder.findPayablePurchases(anyNonNull()))
      .thenReturn(emptyList())

    assertTrue(purchaseBulkPaymentService.loadPayable(searchDto()).isEmpty())
    verify(purchasePaymentCeilingService, never()).computeCeilings(anyNonNull())
    verify(supplierPaymentDataFetcher, never()).getPaidAmountsByPurchaseIds(anyNonNull())
  }
}
