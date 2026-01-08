package me.ezra_home.retail_software_solution.organizations.business.organization_user.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.organizations.model.OrganizationUserEntity}
 */
data class OrganizationUserResponseDto(
    val id: UUID?,
    val joinRequestId: UUID?,
    val user: String?,
    @JsonSerialize(using = DatesToMillis::class)
    val startOn: OffsetDateTime?,
    @JsonSerialize(using = DatesToMillis::class)
    val endOn: OffsetDateTime?,
    val referenceNumber: String?
) : Serializable
