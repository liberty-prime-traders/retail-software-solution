package me.ezra_home.retail_software_solution.platform.business.authorization_pass.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.session.ServiceAccountContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassEntity
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassMapper
import me.ezra_home.retail_software_solution.platform.business.authorization_pass.AuthorizationPassRepository
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import me.ezra_home.retail_software_solution.util.exceptions.UpdatingNonExistingRecordException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.util.UUID

@Service
@TransactionalOnPlatformSchema
class AuthorizationPassService(
    private val authorizationPassRepository: AuthorizationPassRepository,
    private val authorizationPassMapper: AuthorizationPassMapper
) {

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getAllPasses(): List<AuthorizationPassResponseDto> {
        return authorizationPassRepository.findAll().map { authorizationPassMapper.toResponseDto(it) }
    }

    @TransactionalOnPlatformSchema(readOnly = true)
    fun getSecretCode(passRecordId: UUID): UUID {
        return authorizationPassRepository.getReferenceById(passRecordId).code
    }

    fun issue(dto: AuthorizationPassInsertDto): AuthorizationPassResponseDto {
        val entity = AuthorizationPassEntity(
            passType = dto.passType,
            maxUseCount = dto.maxUseCount,
            assignedToId = dto.assignedToId,
            expiresOn = dto.expiresOn
        )
        authorizationPassRepository.save(entity)
        return authorizationPassMapper.toResponseDto(entity)
    }

    fun update(dto: AuthorizationPassUpdateDto): AuthorizationPassResponseDto {
        val pass = authorizationPassRepository.findById(dto.id)
            .orElseThrow { UpdatingNonExistingRecordException() }

        if (pass.passStatus != PassStatus.ACTIVE) {
            throw RtsGenericException("Only active passes can be updated")
        }
        dto.maxUseCount?.ifPresent { newMax ->
            if (newMax < pass.usedCount) {
                throw RtsGenericException("max_use_count cannot be less than current used count (${pass.usedCount})")
            }
            pass.maxUseCount = newMax
            if (pass.usedCount >= newMax) pass.passStatus = PassStatus.EXHAUSTED
        }

        dto.expiresOn?.let { pass.expiresOn = it.orElse(null) }

        authorizationPassRepository.save(pass)
        return authorizationPassMapper.toResponseDto(pass)
    }

    fun revoke(id: UUID): AuthorizationPassResponseDto {
        val pass = authorizationPassRepository.findById(id)
            .orElseThrow { UpdatingNonExistingRecordException() }
        if (pass.passStatus != PassStatus.ACTIVE) {
            throw RtsGenericException("Only active passes can be revoked")
        }
        pass.passStatus = PassStatus.REVOKED
        authorizationPassRepository.save(pass)
        return authorizationPassMapper.toResponseDto(pass)
    }

    fun redeem(code: UUID, passType: PassType): AuthorizationPassEntity {
        val requestingUserId = SessionContextProvider.getUserId()

        val pass = authorizationPassRepository.findByCode(code)
            ?: throw RtsGenericException("Invalid pass code")

        if (pass.passType != passType) {
            throw RtsGenericException("Pass is not valid for this operation")
        }

        if (pass.passStatus != PassStatus.ACTIVE) {
            throw RtsGenericException("Pass is no longer active")
        }

        if (pass.expiresOn != null && pass.expiresOn!!.isBefore(OffsetDateTime.now())) {
            pass.passStatus = PassStatus.EXPIRED
            authorizationPassRepository.save(pass)
            throw RtsGenericException("Pass has expired")
        }

        if (pass.assignedToId != requestingUserId) {
            revokeAndReissue(pass)
            throw RtsGenericException("Pass is not assigned to requesting user.")
        }

        if (pass.usedCount >= pass.maxUseCount) {
            pass.passStatus = PassStatus.EXHAUSTED
            authorizationPassRepository.save(pass)
            throw RtsGenericException("Maximum use count for this pass has been reached")
        }

        pass.usedCount++
        if (pass.usedCount >= pass.maxUseCount) {
            pass.passStatus = PassStatus.EXHAUSTED
        }
        authorizationPassRepository.save(pass)
        return pass
    }

    private fun revokeAndReissue(stolen: AuthorizationPassEntity) {
        ServiceAccountContext.runWithServiceAccount(ServiceAccount.SECURITY_MONITOR) {
            stolen.passStatus = PassStatus.REVOKED
            val regenerated = AuthorizationPassEntity(
                passType = stolen.passType,
                maxUseCount = stolen.maxUseCount,
                assignedToId = stolen.assignedToId,
                expiresOn = stolen.expiresOn
            )
            authorizationPassRepository.saveAll(listOf(stolen, regenerated))
        }
    }
}
