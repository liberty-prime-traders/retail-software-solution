package me.ezra_home.retail_software_solution.platform.business.sysuser

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.UserType
import me.ezra_home.retail_software_solution.util.enums.ServiceAccount
import org.springframework.stereotype.Component

@Component
@TransactionalOnPlatformSchema
class ServiceAccountInserter(private val sysUserRepository: SysUserRepository) {

    fun seed() {
        val serviceAccountEntries = ServiceAccount.entries
        val serviceAccountIds = serviceAccountEntries.map { it.uniqueId }.toSet()
        val existingAccounts = sysUserRepository.findAllById(serviceAccountIds).associateBy { it.id }
        val newAccounts =  serviceAccountEntries
            .filter { it.uniqueId !in existingAccounts }
            .map { account ->
                val entity =  SysUserEntity(userType = UserType.SERVICE_ACCOUNT).also { it.id = account.uniqueId }
                entity.oktaId = account.name
                entity.userType = UserType.SERVICE_ACCOUNT
                entity.localFirstName = account.displayName
                entity
            }
        sysUserRepository.saveAll(newAccounts)
    }
}
