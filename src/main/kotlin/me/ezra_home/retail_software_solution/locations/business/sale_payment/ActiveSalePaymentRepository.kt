package me.ezra_home.retail_software_solution.locations.business.sale_payment

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ActiveSalePaymentRepository : JpaRepository<ActiveSalePaymentEntity, UUID> {

    fun findBySaleIdIn(saleIds: Collection<UUID>): List<ActiveSalePaymentEntity>

    fun existsBySaleId(saleId: UUID): Boolean
}
