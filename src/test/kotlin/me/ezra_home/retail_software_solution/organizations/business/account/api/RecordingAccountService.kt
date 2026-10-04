package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountCache
import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import me.ezra_home.retail_software_solution.organizations.business.account.AccountRepository
import me.ezra_home.retail_software_solution.organizations.business.account.AccountResponseBuilder
import me.ezra_home.retail_software_solution.organizations.business.account.ChildAccountCreator
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.lang.reflect.Proxy
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

/** A real AccountService over every SystemAccount, recording each balance delta it would persist. */
class RecordingAccountService(accountDtos: List<AccountDto> = systemAccountDtos()) {

    private val deltasByAccountCode = mutableMapOf<String, BigDecimal>()

    val accountService: AccountService

    init {
        val accountCache = mock(AccountCache::class.java)
        `when`(accountCache.getAll()).thenReturn(accountDtos)
        `when`(accountCache.getAllFresh()).thenReturn(accountDtos)
        val recordingAccountRepository = Proxy.newProxyInstance(
            AccountRepository::class.java.classLoader,
            arrayOf(AccountRepository::class.java)
        ) { _, method, arguments ->
            if (method.name == "incrementBalance") {
                deltasByAccountCode.merge(arguments[0] as String, arguments[1] as BigDecimal, BigDecimal::add)
            }
            null
        } as AccountRepository
        accountService = AccountService(
            accountCache,
            recordingAccountRepository,
            mock(AccountResponseBuilder::class.java),
            mock(ChildAccountCreator::class.java),
            mock(AccountStructureLock::class.java)
        )
    }

    fun balance(systemAccount: SystemAccount): BigDecimal = deltasByAccountCode[systemAccount.code] ?: BigDecimal.ZERO

    companion object {
        fun systemAccountDtos(): List<AccountDto> = SystemAccount.entries.map { systemAccount ->
            accountDto(systemAccount, accountIsActive = true)
        }

        fun accountDto(systemAccount: SystemAccount, accountIsActive: Boolean) = AccountDto(
            id = UUID.randomUUID(),
            createdById = UUID.randomUUID(),
            createdOn = OffsetDateTime.now(),
            code = systemAccount.code,
            name = systemAccount.accountName,
            accountType = systemAccount.type,
            currencyCode = "USD",
            accountIsActive = accountIsActive,
            accountIsSystemMaintained = true,
            parentAccountCode = systemAccount.parent?.code
        )
    }
}
