package me.ezra_home.retail_software_solution.platform.business.identity

import me.ezra_home.retail_software_solution.platform.business.identity.api.IdentityProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PendingIdentityLinkRepository : JpaRepository<PendingIdentityLinkEntity, UUID> {
    fun findByUserIdAndProviderAndStatus(
        userId: UUID,
        provider: IdentityProvider,
        status: PendingLinkStatus
    ): List<PendingIdentityLinkEntity>
}
