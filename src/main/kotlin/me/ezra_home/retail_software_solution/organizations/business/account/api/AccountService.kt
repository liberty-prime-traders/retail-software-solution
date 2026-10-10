package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountAssertions
import me.ezra_home.retail_software_solution.organizations.business.account.AccountCodeGenerator
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.organizations.business.account.AccountResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.account.ChildAccountCreator
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
    private val childAccountCreator: ChildAccountCreator,
    private val accountStructureLock: AccountStructureLock,
    private val accountUsagesFinder: AccountUsagesFinder
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
        accountStructureLock.acquire(dto.parentAccountCode)
        val accounts = accountCache.getAllFresh()
        val accountsByCode = accounts.associateBy { it.code }
        val newAccount = childAccountCreator.createChild(dto, accountsByCode)
        return accountResponseBuilder.buildResponse(newAccount)
    }

    fun rename(dto: AccountUpdateDto): AccountResponseDto {
        val existing = lockAndGetFresh(dto.id)
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be renamed")
        }
        val saved = accountCache.update(dto.applyTo(existing))
        return accountResponseBuilder.buildResponse(saved)
    }

    fun toggleActive(id: UUID, setActive: Boolean): AccountResponseDto {
        val existing = lockAndGetFresh(id)
        if (existing.accountIsSystemMaintained) {
            throw RtsGenericException("System accounts cannot be activated or deactivated")
        }
        if (existing.accountIsActive == setActive) {
            return accountResponseBuilder.buildResponse(existing)
        }
        if (!setActive) {
            accountUsagesFinder.failOnUsagesForCode(existing.code, "be deactivated")
        }
        val saved = accountCache.update(existing.copy(accountIsActive = setActive))
        return accountResponseBuilder.buildResponse(saved)
    }

    // The code is immutable per id, so the cached lookup is safe; the row itself is re-read once the lock is held.
    private fun lockAndGetFresh(id: UUID): AccountDto {
        val accountCode = accountCache.getAll().firstOrNull { it.id == id }?.code
            ?: throw RtsGenericException("Account not found")
        accountStructureLock.acquire(accountCode)
        return accountCache.getAllFresh().first { it.id == id }
    }

    @TransactionalOnOrganizationSchema(readOnly = true)
    fun assertPostable(accountPostings: List<AccountPosting>) {
        AccountAssertions.assertPostable(accountCache.getAllFresh(), accountPostings)
    }

    fun patchBalances(accountPostings: List<AccountPosting>) {
        val accountsByCode = accountCache.getAll().associateBy { it.code }
        accountPostings.forEach { accountPosting ->
            val account = accountsByCode.getValue(accountPosting.accountCode)
            val delta = if (account.accountType.normalBalance == accountPosting.entryType) accountPosting.amount else accountPosting.amount.negate()
            accountRepository.incrementBalance(account.code, delta, Instant.now())
        }
    }
}
