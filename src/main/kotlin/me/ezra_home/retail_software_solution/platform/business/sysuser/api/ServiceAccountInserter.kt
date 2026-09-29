package me.ezra_home.retail_software_solution.platform.business.sysuser.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserEntity
import me.ezra_home.retail_software_solution.platform.business.sysuser.SysUserRepository
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
                SysUserEntity(userType = UserType.SERVICE_ACCOUNT, localFirstName = account.displayName)
                    .also { it.id = account.uniqueId }
            }
        sysUserRepository.saveAll(newAccounts)
    }
}
