package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountResponseDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class AccountResponseBuilder(
    private val accountRepository: AccountRepository
) {

    fun buildResponse(accounts: AccountDto): AccountResponseDto {
        return buildResponse(listOf(accounts)).first()
    }

    fun buildResponse(accounts: List<AccountDto>): List<AccountResponseDto> {
        val accountsByCode = accounts.associateBy { it.code }
        val rawBalances = accountRepository
            .findBalancesByCodes(accountsByCode.keys.toSet())
            .associateBy(AccountBalance::code)
        val rolledUpBalances = accountsByCode.keys.associateWith { code ->
            computeRolledUpBalance(code, accountsByCode, rawBalances)
        }
        return accounts.map { account -> buildFinalObject(account, accountsByCode, rolledUpBalances) }
    }

    private fun computeRolledUpBalance(
        code: String,
        accountsByCode: Map<String, AccountDto>,
        rawBalances: Map<String, AccountBalance>
    ): BigDecimal {
        val children = accountsByCode.values.filter { it.parentAccountCode == code }
        return if (children.isEmpty()) {
            rawBalances[code]?.currentBalance ?: BigDecimal.ZERO
        } else {
            children.fold(BigDecimal.ZERO) { acc, child ->
                acc + computeRolledUpBalance(child.code, accountsByCode, rawBalances)
            }
        }
    }

    private fun buildFinalObject(
        account: AccountDto,
        accountsByCode: Map<String, AccountDto>,
        rolledUpBalances: Map<String, BigDecimal>
    ): AccountResponseDto {
        val currentBalance = rolledUpBalances[account.code] ?: BigDecimal.ZERO
        val isExtensible = isExtensible(account, accountsByCode[account.parentAccountCode])
        val balanceSignal = when {
            BigDecimal.ZERO.compareTo(currentBalance) == 0 -> BalanceSignal.ZERO_BALANCE
            currentBalance < BigDecimal.ZERO -> BalanceSignal.IRREGULAR_BALANCE
            else -> account.accountType.balanceSignal()
        }
        return AccountResponseDto(
            id = account.id,
            code = account.code,
            name = account.name,
            displayName = account.label,
            accountType = account.accountType,
            accountIsActive = account.accountIsActive,
            accountIsSystemMaintained = account.accountIsSystemMaintained,
            currentBalance = currentBalance,
            parentAccountCode = account.parentAccountCode,
            parentAccount = accountsByCode[account.parentAccountCode]?.label,
            accountIsExtensible = isExtensible,
            balanceSignal = balanceSignal
        )
    }

    fun isExtensible(accountDto: AccountDto, parentAccountDto: AccountDto?): Boolean {
        if (accountDto.accountIsSystemMaintained) {
            return SystemAccount.fromCode(accountDto.code)?.isExtensible() ?: false
        }
        val parentSystemAccount = parentAccountDto?.let { SystemAccount.fromCode(it.code) }
        return parentSystemAccount?.isExtensible() != true
    }
}
