package me.ezra_home.retail_software_solution.platform.business.identity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.configuration.security.IdentityProviderConverter
import me.ezra_home.retail_software_solution.platform.business.identity.api.IdentityProvider
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.IDENTITY_PROVIDER_LINK)
class IdentityProviderLinkEntity(

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "provider", nullable = false, length = 5)
    @Convert(converter = IdentityProviderConverter::class)
    var provider: IdentityProvider,

    @Column(name = "external_id", nullable = false, length = 255)
    var externalId: String,

    @Column(name = "linked_at", nullable = false)
    var linkedAt: OffsetDateTime

) : BaseEntity()
