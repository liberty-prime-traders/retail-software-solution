package me.ezra_home.retail_software_solution.locations.business.purchase.api

import me.ezra_home.retail_software_solution.util.enums.PaymentStatus
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnLocationSchema
import me.ezra_home.retail_software_solution.locations.business.lock.api.EntityAdvisoryLock
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseAssembler
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseEntity
import me.ezra_home.retail_software_solution.locations.business.purchase.PurchaseRepository
import me.ezra_home.retail_software_solution.util.business.lock.LockNamespaces
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import java.util.UUID

@Service
@TransactionalOnLocationSchema(readOnly = true)
class PurchaseDataFetcher(
  private val purchaseRepository: PurchaseRepository,
  private val purchaseAssembler: PurchaseAssembler,
  private val entityAdvisoryLock: EntityAdvisoryLock
) {

  data class PurchaseInfo(
    val id: UUID,
    val referenceNumber: String,
    val supplierId: UUID,
    val purchaseStatus: PurchaseStatus,
    val paymentStatus: PaymentStatus
  )

  fun fetchTop(n: Int?): List<PurchaseResponseDto> {
    val recordCount = n ?: 10
    if (recordCount > 1000) throw RtsGenericException("Limit exceeds maximum of 1000")
    val sort = Sort.by(Sort.Direction.DESC, "createdOn")
    return purchaseAssembler.buildResponses(purchaseRepository.findTopN(PageRequest.of(0, recordCount, sort)))
  }

  fun findPurchaseInfoByReferenceNumber(referenceNumber: String): PurchaseInfo {
    val purchase = purchaseRepository.findByReferenceNumber(referenceNumber)
      ?: throw RtsGenericException("Purchase $referenceNumber not found")
    return purchase.toPurchaseInfo()
  }

  fun getSupplierId(purchaseId: UUID): UUID {
    return purchaseRepository.getReferenceById(purchaseId).supplierId
  }


  fun findPurchaseInfoByIds(purchaseIds: List<UUID>): Map<UUID, PurchaseInfo> {
    return purchaseRepository.findAllById(purchaseIds)
      .associateBy({ it.id!! }, { it.toPurchaseInfo() })
  }

  @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
  fun lockPurchase(purchaseId: UUID) {
    entityAdvisoryLock.acquire(LockNamespaces.PURCHASE, purchaseId)
  }

  @TransactionalOnLocationSchema(propagation = Propagation.MANDATORY)
  fun lockAndGetPurchase(purchaseId: UUID): PurchaseEntity {
    entityAdvisoryLock.acquire(LockNamespaces.PURCHASE, purchaseId)
    return purchaseRepository.getReferenceById(purchaseId)
  }

  private fun PurchaseEntity.toPurchaseInfo() =
    PurchaseInfo(id!!, requiredReference(), supplierId, purchaseStatus, paymentStatus)
}
