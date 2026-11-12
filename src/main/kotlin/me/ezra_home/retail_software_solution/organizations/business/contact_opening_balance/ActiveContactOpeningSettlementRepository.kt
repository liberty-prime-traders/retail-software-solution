package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.UUID

@Repository
interface ActiveContactOpeningSettlementRepository : JpaRepository<ActiveContactOpeningSettlementEntity, UUID> {

    @Query(
        """
        SELECT COALESCE(SUM(active.amount), 0) FROM ActiveContactOpeningSettlementEntity active
        WHERE active.contactReferenceNumber = :contactReferenceNumber AND active.subledgerDirection = :subledgerDirection
        """
    )
    fun sumAmount(contactReferenceNumber: String, subledgerDirection: ContactSubledgerDirection): BigDecimal
}
