package me.ezra_home.retail_software_solution.platform.business.identity

import me.ezra_home.retail_software_solution.platform.business.identity.api.IdentityProvider
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface IdentityProviderLinkRepository : JpaRepository<IdentityProviderLinkEntity, UUID> {
    fun findByProviderAndExternalId(provider: IdentityProvider, externalId: String): IdentityProviderLinkEntity?
}
