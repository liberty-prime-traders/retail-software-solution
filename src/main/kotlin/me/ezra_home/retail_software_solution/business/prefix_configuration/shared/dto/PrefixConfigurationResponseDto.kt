package me.ezra_home.retail_software_solution.business.prefix_configuration.shared.dto

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import me.ezra_home.retail_software_solution.configuration.serializer.DatesToMillis
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class PrefixConfigurationResponseDto(
    val id: UUID?,
    val tableRegistryId: UUID?,
    val prefix: String?,
    val createdBy: String?,
    @JsonSerialize(using = DatesToMillis::class)
    val createdOn: OffsetDateTime?
) : Serializable
