package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCodeGenerator
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.organizations.business.account.AccountResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.account.ChildAccountCreator
import me.ezra_home.retail_software_solution.organizations.business.ledger.api.LedgerEntrySummaryDto
import me.ezra_home.retail_software_solution.util.business.StringUtils
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID


@Service
@TransactionalOnOrganizationSchema
class AccountService(
    private val accountCache: AccountCache,
    private val accountRepository: AccountRepository,
    private val accountResponseBuilder: AccountResponseBuilder,
    private val childAccountCreator: ChildAccountCreator
) {

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAll(): List<AccountResponseDto> {
        return accountResponseBuilder.buildResponse(accountCache.getAll())
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun getAccountNamesByCode(): Map<String, String> {
        return accountCache.getAll().associate { it.code to it.label }
    }

    fun createRoot(dto: AccountRootCreateRequest): AccountResponseDto {
        val accountType = dto.accountType
        if (accountType.canBeRoot().not()) {
            throw RtsGenericException("Only certain account types can be created as root accounts and $accountType is not one of them")
        }
        val accounts = accountCache.getAll()
        accounts.find { it.parentAccountCode == null && StringUtils.isEquivalent(it.name, dto.name) }
            ?.let { throw RtsGenericException("A root account with the same name already exists") }

        val code = AccountCodeGenerator.generateRootCode(accounts)
        val insertDto = AccountInsertDto(
            code = code,
            name = dto.name,
            accountType = accountType
        )
        val saved = accountCache.create(insertDto)
        return accountResponseBuilder.buildResponse(saved)
    }

    fun createChild(dto: AccountChildCreateRequest): AccountResponseDto {
        val accounts = accountCache.getAll()
        val accountsByCode = accounts.associateBy { it.code }
        val newAccount = childAccountCreator.createChild(dto, accountsByCode)
        return accountResponseBuilder.buildResponse(newAccount)
    }

    fun rename(dto: AccountUpdateDto): AccountResponseDto {
        val accounts = accountCache.getAll()
        val existing = accounts.firstOrNull { it.id == dto.id }
            ?: throw RtsGenericException("Account not found")
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be renamed")
        }
        val saved = accountCache.update(dto.applyTo(existing))
        return accountResponseBuilder.buildResponse(saved)
    }

    fun toggleActive(id: UUID, setActive: Boolean): AccountResponseDto {
        val accounts = accountCache.getAll()
        val existing = accounts.firstOrNull { it.id == id }
            ?: throw RtsGenericException("Account not found")
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be activated or deactivated")
        }
        if (existing.accountIsActive == setActive) {
            return accountResponseBuilder.buildResponse(existing)
        }
        val saved = accountCache.update(existing.copy(accountIsActive = setActive))
        return accountResponseBuilder.buildResponse(saved)
    }

    fun patchBalances(entries: List<LedgerEntrySummaryDto>) {
        val accountsByCode = accountCache.getAll().associateBy { it.code }
        entries.forEach { entry ->
            val account = accountsByCode[entry.accountCode] ?: return@forEach
            val delta = if (account.accountType.normalBalance == entry.entryType) entry.amount else entry.amount.negate()
            accountRepository.incrementBalance(account.code, delta, Instant.now())
        }
    }
}
