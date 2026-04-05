package me.ezra_home.retail_software_solution.platform.business.reserved_subdomain

import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.ReservedSubdomainEntity
import me.ezra_home.retail_software_solution.platform.business.reserved_subdomain.`public`.ReservedDomainStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface SubdomainRepository : JpaRepository<ReservedSubdomainEntity, UUID> {

    fun findByStatusNotAndSubdomain(reservedDomainStatus: ReservedDomainStatus, subdomain: String): List<ReservedSubdomainEntity>

    @Modifying
    @Query("update ReservedSubdomainEntity r set r.status = 'ABND' where r.createdById = :createdById and r.status = 'UNSD'")
    fun abandonSubdomainsForUser(@Param("createdById") createdById: UUID)
}
