package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountChildCreateRequest
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountInsertDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountUsagesFinder
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChildAccountCreator(
    private val accountCache: AccountCache,
    private val accountUsagesFinder: AccountUsagesFinder
) {

    fun createChild(dto: AccountChildCreateRequest, accountsById: Map<UUID, AccountDto>): AccountDto {
        val parentId = dto.parentAccountId
        val parent = accountsById[parentId] ?: throw RtsGenericException("Parent account not found")
        val siblings = accountsById.values.filter { it.parentAccountId == parentId }

        runValidations(dto, parent, siblings, accountsById)

        val insertDto = AccountInsertDto(
            code = AccountCodeGenerator.generateChildCode(parent.code, siblings),
            name = dto.name,
            accountType = parent.accountType,
            currencyCode = parent.currencyCode,
            parentAccountId = parentId
        )
        return accountCache.create(insertDto)
    }

    private fun runValidations(
        childCreateRequest: AccountChildCreateRequest,
        parent: AccountDto,
        siblings: List<AccountDto>,
        accountsById: Map<UUID, AccountDto>
    ) {
        ensureParentCanGainChild(parent)
        preventNameCollisionAmongSiblings(childCreateRequest, siblings)
        preventSystemAccountGainingGrandChild(parent, accountsById)
        accountUsagesFinder.failOnUsagesForCode(parent.code)
    }

    private fun ensureParentCanGainChild(parent: AccountDto) {
        if (!parent.accountIsActive) {
            throw RtsGenericException("Cannot add children to inactive account '${parent.name}' (${parent.code})")
        }
        if (parent.accountIsSystemMaintained) {
            val systemAccount = SystemAccount.fromCode(parent.code)
            if (systemAccount?.isExtensible() != true) {
                throw RtsGenericException("${parent.label} is maintained by the system and does not allow child accounts to be added")
            }
        }
    }

    private fun preventSystemAccountGainingGrandChild(parent: AccountDto, accountsById: Map<UUID, AccountDto>) {
        val grandparent = accountsById[parent.parentAccountId] ?: return
        val isSystemAccount = grandparent.accountIsSystemMaintained
        val isExtensible = SystemAccount.fromCode(grandparent.code)?.isExtensible() == true
        if (isSystemAccount && isExtensible) {
            throw RtsGenericException("Cannot add further children under '${parent.label}'")
        }
    }

    private fun preventNameCollisionAmongSiblings(childCreateRequest: AccountChildCreateRequest, siblings: List<AccountDto>) {
        siblings.find { StringUtils.isEquivalent(it.name, childCreateRequest.name) }
            ?.let { throw RtsGenericException("Sibling is already registered with the same name") }
    }


}
