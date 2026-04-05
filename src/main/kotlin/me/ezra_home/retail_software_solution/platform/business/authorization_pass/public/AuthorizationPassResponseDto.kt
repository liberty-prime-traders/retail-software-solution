package me.ezra_home.retail_software_solution.platform.business.authorization_pass.public

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.UUID

data class AuthorizationPassResponseDto(
    val id: UUID,
    val referenceNumber: String,
    val passType: PassType,
    val maxUseCount: Int,
    val usedCount: Int,
    val assignedTo: String?,
    val passStatus: PassStatus,
    val expiresOn: OffsetDateTime?,
    val createdBy: String?,
    val createdOn: OffsetDateTime?
) : Serializable
