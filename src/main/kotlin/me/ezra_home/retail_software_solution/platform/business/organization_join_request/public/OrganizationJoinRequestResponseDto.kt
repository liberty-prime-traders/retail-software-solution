package me.ezra_home.retail_software_solution.platform.business.organization_join_request.public

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationJoinRequestResponseDto(
    val id: UUID,
    val domain: String,
    val status: JoinRequestStatus,

    @field:JsonSerialize(using = DatesToMillis::class)
    val requestedDate: OffsetDateTime,
    val referenceNumber: String?

) : Serializable
