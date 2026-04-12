package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCodeGenerator
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountMapper
import me.ezra_home.retail_software_solution.organizations.business.account.ChildAccountCreator
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.util.UUID


@Service
@TransactionalOnOrganizationSchema
class AccountService(
    private val accountCache: AccountCache,
    private val accountMapper: AccountMapper,
    private val childAccountCreator: ChildAccountCreator
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<AccountResponseDto> {
        val accounts = accountCache.getAll()
        val accountsById: Map<UUID, AccountDto> = accounts.associateBy { it.id }
        return accounts.map {
            accountMapper.toResponseDto(it, accountsById[it.parentAccountId]?.label)
        }
    }

    fun createRoot(dto: AccountRootCreateRequest): AccountResponseDto {
        val accountType = dto.accountType
        if (accountType.canBeRoot().not()) {
            throw RtsGenericException("Only certain account types can be created as root accounts and $accountType is not one of them")
        }
        val accounts = accountCache.getAll()
        accounts.find { it.parentAccountId == null && StringUtils.isEquivalent(it.name, dto.name) }
            ?.let { throw RtsGenericException("A root account with the same name already exists") }

        val code = AccountCodeGenerator.generateRootCode(accounts)
        val insertDto = AccountInsertDto(
            code = code,
            name = dto.name,
            accountType = accountType
        )
        val saved = accountCache.create(insertDto)
        return accountMapper.toResponseDto(saved)
    }

    fun createChild(dto: AccountChildCreateRequest): AccountResponseDto {
        val accounts = accountCache.getAll()
        val accountsById: Map<UUID, AccountDto> = accounts.associateBy { it.id }
        val newAccount = childAccountCreator.createChild(dto, accountsById)
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


}
