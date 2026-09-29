package me.ezra_home.retail_software_solution.locations.business.supplier_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SupplierPaymentVoidRepository : JpaRepository<SupplierPaymentVoidEntity, UUID> {
    fun existsBySupplierPaymentId(supplierPaymentId: UUID): Boolean
    fun findBySupplierPaymentIdIn(supplierPaymentIds: Collection<UUID>): List<SupplierPaymentVoidEntity>
}
