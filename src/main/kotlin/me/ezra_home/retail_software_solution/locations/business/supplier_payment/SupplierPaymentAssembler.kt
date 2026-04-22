package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import me.ezra_home.retail_software_solution.locations.business.delivery.api.PurchaseDeliveryFetcher
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PaymentStatus
import me.ezra_home.retail_software_solution.locations.business.purchase.api.PurchaseDataFetcher
import me.ezra_home.retail_software_solution.locations.business.supplier_payment.api.SupplierPaymentResponseDto
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactService
import me.ezra_home.retail_software_solution.organizations.business.payment_method.api.PaymentMethodService
import me.ezra_home.retail_software_solution.util.business.mappers.UserQualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class SupplierPaymentAssembler(
    private val purchaseDataFetcher: PurchaseDataFetcher,
    private val purchaseDeliveryFetcher: PurchaseDeliveryFetcher,
    private val paymentMethodService: PaymentMethodService,
    private val contactService: ContactService,
    private val userQualifier: UserQualifier
) {

    fun buildResponse(
        payment: SupplierPaymentEntity,
        paymentVoid: SupplierPaymentVoidEntity?,
        paymentStatus: PaymentStatus?
    ): SupplierPaymentResponseDto {
        val purchaseInfo = purchaseDataFetcher.findPurchaseInfoByIds(listOf(payment.purchaseId)).getValue(payment.purchaseId)
        val deliveryRef = payment.deliveryId?.let { purchaseDeliveryFetcher.getDeliveryReferenceNumbersById(listOf(it))[it] }
        val paymentMethodName = paymentMethodService.getAllPaymentMethods().find { it.id == payment.paymentMethodId }?.name ?: ""
        val supplierName = contactService.getContactById(purchaseInfo.supplierId).identity.displayName
        return toDto(payment, purchaseInfo.referenceNumber, deliveryRef, paymentMethodName, supplierName, paymentVoid, paymentStatus)
    }

    fun buildResponses(
        payments: List<SupplierPaymentEntity>,
        voidsByPaymentId: Map<UUID, SupplierPaymentVoidEntity>
    ): List<SupplierPaymentResponseDto> {
        if (payments.isEmpty()) return emptyList()
        val purchaseInfoById = purchaseDataFetcher.findPurchaseInfoByIds(payments.map { it.purchaseId }.distinct())
        val deliveryIds = payments.mapNotNull { it.deliveryId }
        val deliveryRefsById = if (deliveryIds.isNotEmpty()) purchaseDeliveryFetcher.getDeliveryReferenceNumbersById(deliveryIds) else emptyMap()
        val paymentMethodNamesById = paymentMethodService.getAllPaymentMethods().associateBy({ it.id }, { it.name })
        val supplierNamesById = contactService.getAllContactDtos().associateBy({ it.id }, { it.identity.displayName })
        return payments.map { payment ->
            val purchaseInfo = purchaseInfoById.getValue(payment.purchaseId)
            toDto(
                payment,
                purchaseInfo.referenceNumber,
                payment.deliveryId?.let { deliveryRefsById[it] },
                paymentMethodNamesById[payment.paymentMethodId] ?: "",
                supplierNamesById[purchaseInfo.supplierId] ?: "",
                voidsByPaymentId[payment.id!!],
                null
            )
        }
    }

    private fun toDto(
        payment: SupplierPaymentEntity,
        purchaseRef: String,
        deliveryRef: String?,
        paymentMethodName: String,
        supplierName: String,
        paymentVoid: SupplierPaymentVoidEntity?,
        paymentStatus: PaymentStatus?
    ) = SupplierPaymentResponseDto(
        id = payment.id!!,
        referenceNumber = payment.referenceNumber!!,
        purchaseId = payment.purchaseId,
        purchaseReferenceNumber = purchaseRef,
        deliveryReferenceNumber = deliveryRef,
        paymentMethod = paymentMethodName,
        supplier = supplierName,
        amount = payment.amount,
        paymentDate = payment.paymentDate,
        notes = payment.notes,
        createdBy = userQualifier.getUserFullName(payment.createdById) ?: "",
        createdOn = payment.createdOn.toString(),
        voidedReason = paymentVoid?.reason,
        updatedPurchasePaymentStatus = paymentStatus
    )
}
