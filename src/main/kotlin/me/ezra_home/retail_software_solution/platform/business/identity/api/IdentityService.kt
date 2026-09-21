package me.ezra_home.retail_software_solution.platform.business.identity.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.configuration.security.SessionTokenService
import me.ezra_home.retail_software_solution.platform.business.identity.IdentityProviderLinkEntity
import me.ezra_home.retail_software_solution.platform.business.identity.IdentityProviderLinkRepository
import me.ezra_home.retail_software_solution.platform.business.identity.PendingIdentityLinkEntity
import me.ezra_home.retail_software_solution.platform.business.identity.PendingIdentityLinkRepository
import me.ezra_home.retail_software_solution.platform.business.identity.PendingLinkStatus
import me.ezra_home.retail_software_solution.platform.business.permission_assignment.api.PlatformPermissionAssignmentService
import me.ezra_home.retail_software_solution.platform.business.role_assignment.api.PlatformRoleAssignmentService
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.SysUserService
import me.ezra_home.retail_software_solution.util.business.DateTimes
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.AuthException
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID

@Service
class IdentityService(
    private val identityProviderServices: List<IdentityProviderService>,
    private val identityProviderLinkRepository: IdentityProviderLinkRepository,
    private val pendingIdentityLinkRepository: PendingIdentityLinkRepository,
    private val sysUserService: SysUserService,
    private val sessionTokenService: SessionTokenService,
    private val platformRoleAssignmentService: PlatformRoleAssignmentService,
    private val platformPermissionAssignmentService: PlatformPermissionAssignmentService,

    @param:Value("\${rts.pending-identity-link.expiration-minutes}")
    private val pendingLinkExpirationMinutes: Long
) {

    @TransactionalOnPlatformSchema
    fun login(loginRequest: LoginRequest): LoginResponse {
        val identityProviderService = identityProviderServices.find { it.supports(loginRequest.provider) }
            ?: throw AuthException.InvalidCredential()
        val authenticatedIdentity = identityProviderService.authenticate(loginRequest.credential)

        val systemUserId = resolveSystemUserId(authenticatedIdentity)
        val user = sysUserService.findById(systemUserId)
            ?: throw RtsGenericException("System user $systemUserId was resolved during login but no longer exists")
        if (user.disabledAt != null) throw AuthException.AccountDisabled()
        val sessionToken = sessionTokenService.mint(systemUserId)
        val verifiedRoles = verifyRequestedRoles(systemUserId, loginRequest.rolesToVerify)
        val verifiedPermissions = verifyRequestedPermissions(systemUserId, loginRequest.permissionsToVerify)
        return LoginResponse(
            sessionToken = sessionToken,
            user = user,
            verifiedRoles = verifiedRoles,
            verifiedPermissions = verifiedPermissions
        )
    }

    private fun resolveSystemUserId(authenticatedIdentity: AuthenticatedIdentity): UUID {
        identityProviderLinkRepository
            .findByProviderAndExternalId(authenticatedIdentity.provider, authenticatedIdentity.externalId)
            ?.let { return it.userId }

        val existingUserByEmail = authenticatedIdentity.email?.let { sysUserService.findByEmail(it) }
        if (existingUserByEmail == null) {
            val newUserId = sysUserService.createUser(
                authenticatedIdentity.email,
                authenticatedIdentity.firstName,
                authenticatedIdentity.lastName
            )
            linkIdentity(newUserId, authenticatedIdentity)
            return newUserId
        }

        invalidateExistingPendingLinks(existingUserByEmail.id, authenticatedIdentity.provider)
        val pendingLinkToken = createPendingLink(existingUserByEmail.id, authenticatedIdentity)
        throw AuthException.ProviderMismatch(pendingLinkToken)
    }

    private fun linkIdentity(userId: UUID, authenticatedIdentity: AuthenticatedIdentity) {
        identityProviderLinkRepository.save(
            IdentityProviderLinkEntity(
                userId = userId,
                provider = authenticatedIdentity.provider,
                externalId = authenticatedIdentity.externalId,
                linkedAt = DateTimes.Offset.Now.system()
            )
        )
    }

    private fun invalidateExistingPendingLinks(userId: UUID, provider: IdentityProvider) {
        val existingPendingLinks = pendingIdentityLinkRepository
            .findByUserIdAndProviderAndStatus(userId, provider, PendingLinkStatus.PENDING)
        existingPendingLinks.forEach { it.status = PendingLinkStatus.INVALIDATED }
        pendingIdentityLinkRepository.saveAll(existingPendingLinks)
    }

    private fun createPendingLink(userId: UUID, authenticatedIdentity: AuthenticatedIdentity): String {
        val now = DateTimes.Offset.Now.system()
        val publicToken = generatePublicToken()
        pendingIdentityLinkRepository.save(
            PendingIdentityLinkEntity(
                publicToken = publicToken,
                userId = userId,
                provider = authenticatedIdentity.provider,
                externalId = authenticatedIdentity.externalId,
                status = PendingLinkStatus.PENDING,
                expiresAt = now.plusMinutes(pendingLinkExpirationMinutes),
                createdAt = now
            )
        )
        return publicToken
    }

    private fun generatePublicToken(): String {
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes)
    }

    // Unknown names and roles/permissions the user simply doesn't hold are both silently dropped:
    // distinguishing them would let an unauthenticated caller probe for valid names.
    private fun verifyRequestedRoles(
        userId: UUID,
        requestedRoleNames: List<String>
    ): List<RtsRole> {
        val heldPlatformRoles = platformRoleAssignmentService.getRoles(userId)
        return requestedRoleNames
            .mapNotNull { name -> runCatching { RtsRole.valueOf(name) }.getOrNull() }
            .filter { it.tier == SchemaLevel.PLATFORM && it in heldPlatformRoles }
    }

    private fun verifyRequestedPermissions(
        userId: UUID,
        requestedPermissionNames: List<String>
    ): List<RtsPermission> {
        val heldPlatformPermissions = platformPermissionAssignmentService.getPermissions(userId)
        return requestedPermissionNames
            .mapNotNull { name -> runCatching { RtsPermission.valueOf(name) }.getOrNull() }
            .filter { it.tier == SchemaLevel.PLATFORM && it in heldPlatformPermissions }
    }
}
