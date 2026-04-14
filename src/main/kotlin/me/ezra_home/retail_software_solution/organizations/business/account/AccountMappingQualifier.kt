package me.ezra_home.retail_software_solution.organizations.business.account

import org.mapstruct.Context
import org.mapstruct.Qualifier
import org.springframework.stereotype.Component

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class IsExtensible

@Qualifier
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ParentAccountLabel

@Component
class AccountMappingQualifier {

    @IsExtensible
    fun mapIsExtensible(accountDto: AccountDto, @Context parentAccountDto: AccountDto?): Boolean {
        if (accountDto.accountIsSystemMaintained) {
            return SystemAccount.fromCode(accountDto.code)?.isExtensible() ?: false
        }
        val parentSystemAccount = parentAccountDto?.let { SystemAccount.fromCode(it.code) }
        return parentSystemAccount?.isExtensible() != true
    }

    @ParentAccountLabel
    fun getParentAccountLabel(dto: AccountDto, @Context parentAccountDto: AccountDto?): String? {
        return parentAccountDto?.label
    }
}
