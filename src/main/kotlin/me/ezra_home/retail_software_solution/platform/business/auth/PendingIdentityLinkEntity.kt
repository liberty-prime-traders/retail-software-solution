package me.ezra_home.retail_software_solution.platform.business.auth

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.configuration.security.IdentityProviderConverter
import me.ezra_home.retail_software_solution.platform.business.auth.api.IdentityProvider
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = TableNames.PENDING_IDENTITY_LINK)
class PendingIdentityLinkEntity(

    @Column(name = "public_token", nullable = false, length = 255)
    var publicToken: String,

    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Column(name = "provider", nullable = false, length = 5)
    @Convert(converter = IdentityProviderConverter::class)
    var provider: IdentityProvider,

    @Column(name = "external_id", nullable = false, length = 255)
    var externalId: String,

    @Column(name = "status", nullable = false, length = 5)
    @Convert(converter = PendingLinkStatusConverter::class)
    var status: PendingLinkStatus,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime,

    @Column(name = "consumed_at")
    var consumedAt: OffsetDateTime? = null

) : BaseEntity()
