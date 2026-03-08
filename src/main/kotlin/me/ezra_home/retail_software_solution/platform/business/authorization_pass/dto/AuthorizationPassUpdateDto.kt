package me.ezra_home.retail_software_solution.platform.business.authorization_pass.dto

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.Optional
import java.util.UUID

data class AuthorizationPassUpdateDto(
    val id: UUID,
    val maxUseCount: Optional<Int>? = null,
    val expiresOn: Optional<OffsetDateTime>? = null
) : Serializable
