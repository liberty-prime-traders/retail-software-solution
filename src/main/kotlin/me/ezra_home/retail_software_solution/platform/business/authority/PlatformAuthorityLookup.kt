package me.ezra_home.retail_software_solution.platform.business.authority

import me.ezra_home.retail_software_solution.cross_tier.authority.AuthorityType
import me.ezra_home.retail_software_solution.util.enums.RtsPermission
import me.ezra_home.retail_software_solution.util.enums.RtsRole
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

sealed interface PlatformAuthorityLookup {
    data class ByRole(val role: RtsRole) : PlatformAuthorityLookup
    data class ByPermission(val permission: RtsPermission) : PlatformAuthorityLookup

    companion object {
        fun of(authorityName: String, authorityType: AuthorityType): PlatformAuthorityLookup = when (authorityType) {
            AuthorityType.ROLE -> ByRole(resolvePlatformRole(authorityName))
            AuthorityType.PERMISSION -> ByPermission(resolvePlatformPermission(authorityName))
        }

        private fun resolvePlatformRole(authorityName: String): RtsRole {
            val role = runCatching { RtsRole.valueOf(authorityName) }.getOrNull()
                ?: throw RtsGenericException("$authorityName is not a known role")
            if (role.tier != SchemaLevel.PLATFORM) {
                throw RtsGenericException("$authorityName is not a platform-tier role")
            }
            return role
        }

        private fun resolvePlatformPermission(authorityName: String): RtsPermission {
            val permission = runCatching { RtsPermission.valueOf(authorityName) }.getOrNull()
                ?: throw RtsGenericException("$authorityName is not a known permission")
            if (permission.tier != SchemaLevel.PLATFORM) {
                throw RtsGenericException("$authorityName is not a platform-tier permission")
            }
            return permission
        }
    }
}
