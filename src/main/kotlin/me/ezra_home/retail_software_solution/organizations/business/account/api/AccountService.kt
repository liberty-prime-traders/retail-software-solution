package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountMapper
import me.ezra_home.retail_software_solution.organizations.business.account.AccountValidator
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TransactionalOnOrganizationSchema
class AccountService(
    private val accountCache: AccountCache,
    private val accountMapper: AccountMapper
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<AccountResponseDto> {
        val accounts = accountCache.getAll()
        val accountsById: Map<UUID, AccountDto> = accounts.associateBy { it.id }
        return accounts.map {
            accountMapper.toResponseDto(it, accountsById[it.parentAccountId]?.label)
        }
    }

    fun create(dto: AccountCreateRequest): AccountResponseDto {
        val newAccount = createChild(dto)
        val accountsById: Map<UUID, AccountDto> = accountCache.getAll().associateBy { it.id }
        return accountMapper.toResponseDto(newAccount, accountsById[newAccount.parentAccountId]?.label)
    }

    fun rename(dto: AccountUpdateDto): AccountResponseDto {
        val accounts = accountCache.getAll()
        val existing = accounts.firstOrNull { it.id == dto.id }
            ?: throw RtsGenericException("Account not found")
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be renamed")
        }
        val saved = accountCache.update(dto.applyTo(existing))
        val accountsById: Map<UUID, AccountDto> = accounts.associateBy { it.id }
        return accountMapper.toResponseDto(saved, accountsById[saved.parentAccountId]?.label)
    }

    fun toggleActive(id: UUID, setActive: Boolean): AccountResponseDto {
        val accounts = accountCache.getAll()
        val existing = accounts.firstOrNull { it.id == id }
            ?: throw RtsGenericException("Account not found")
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be activated or deactivated")
        }
        val accountsById: Map<UUID, AccountDto> = accounts.associateBy { it.id }
        val parentLabel = accountsById[existing.parentAccountId]?.label
        if (existing.accountIsActive == setActive) {
            return accountMapper.toResponseDto(existing, parentLabel)
        }
        val saved = accountCache.update(existing.copy(accountIsActive = setActive))
        return accountMapper.toResponseDto(saved, parentLabel)
    }

    private fun createChild(dto: AccountCreateRequest): AccountDto {
        val accounts = accountCache.getAll()
        val accountsById: Map<UUID, AccountDto> = accounts.associateBy { it.id }
        val parentId = dto.parentAccountId
        val parent = accountsById[parentId]
            ?: throw RtsGenericException("Parent account not found")

        val children = accounts.filter { it.parentAccountId == parentId }
        AccountValidator.validateForChildCreate(parent, children)

        val insertDto = AccountInsertDto(
            code = generateChildCode(parent.code, children),
            name = dto.name,
            accountType = parent.accountType,
            currencyCode = parent.currencyCode,
            parentAccountId = parentId
        )
        return accountCache.create(insertDto)
    }

    private fun generateChildCode(parentCode: String, children: List<AccountDto>): String {
        val prefix = parentCode.take(2)
        val parentNumeric = parentCode.drop(2).toInt()
        val step = if (parentNumeric % 1000 == 0) 100 else 10
        val highestChild = children.maxOfOrNull { it.code.drop(2).toInt() } ?: parentNumeric
        return "$prefix${highestChild + step}"
    }
}
