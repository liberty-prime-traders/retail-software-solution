package me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto

import me.ezra_home.retail_software_solution.util.enums.PassType
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class AuthorizationPassInsertDto(
    val passType: PassType,
    val maxUseCount: Int,
    val assignedToId: UUID,
    val expiresOn: OffsetDateTime? = null
) : Serializable
