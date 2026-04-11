package me.ezra_home.retail_software_solution.organizations.business.account

import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class IsExtensible

@Component
class AccountMappingQualifier {

    @IsExtensible
    fun mapIsExtensible(accountDto: AccountDto): Boolean {
        return SystemAccount.fromCode(accountDto.code)?.isExtensible() ?: false
    }
}
