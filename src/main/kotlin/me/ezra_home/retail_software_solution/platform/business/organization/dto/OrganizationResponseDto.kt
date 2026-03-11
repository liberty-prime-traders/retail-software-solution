package me.ezra_home.retail_software_solution.platform.business.organization.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class OrganizationResponseDto(
    val id: UUID?,
    val createdBy: String?,
    @field:JsonSerialize(using = DatesToMillis::class)
    val createdOn: OffsetDateTime?,
    val name: String?,
    val description: String?,
    val hidden: Boolean,
    val currentDbVersion: String?,
    val creationPassReferenceNumber: String?,
    val subdomain: String?,
    val referenceNumber: String?
) : Serializable
