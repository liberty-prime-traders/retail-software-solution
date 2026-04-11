package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

private const val MAX_CHILDREN = 9

object AccountValidator {

    fun validateForChildCreate(parent: AccountDto, children: List<AccountDto>) {
        if (!parent.accountIsActive) {
            throw RtsGenericException("Cannot add children to inactive account '${parent.name}' (${parent.code})")
        }
        if (children.size >= MAX_CHILDREN) {
            throw RtsGenericException("Account '${parent.name}' (${parent.code}) already has $MAX_CHILDREN children")
        }

        val systemAccountEquivalent = SystemAccount.fromCode(parent.code)
        if (systemAccountEquivalent?.isExtensible() != true) {
            throw RtsGenericException("Cannot add children to account '${parent.name}' (${parent.code})")
        }
    }

}
