package me.ezra_home.retail_software_solution.organizations.business.account

import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnOrganizationSchema
import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountResponseDto
import me.ezra_home.retail_software_solution.organizations.business.account.api.SystemAccount
import me.ezra_home.retail_software_solution.organizations.business.opening_balance.api.OpeningBalanceService
import me.ezra_home.retail_software_solution.util.ui_models.BalanceSignal
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
@TransactionalOnOrganizationSchema(readOnly = true)
class AccountResponseBuilder(
    private val accountRepository: AccountRepository,
    private val openingBalanceService: OpeningBalanceService
) {

    fun buildResponse(accounts: AccountDto): AccountResponseDto {
        return buildResponse(listOf(accounts)).first()
    }

    fun buildResponse(accounts: List<AccountDto>): List<AccountResponseDto> {
        val accountsByCode = accounts.associateBy { it.code }
        val rawBalances = accountRepository
            .findBalancesByCodes(accountsByCode.keys.toSet())
            .associateBy(AccountBalance::code)
        val rawOpeningBalances = openingBalanceService.getAmountsByAccountCodes(accountsByCode.keys)
        val rolledUpBalances = accountsByCode.keys.associateWith { code ->
            computeRolledUpAmount(code, accountsByCode) { rawBalances[it]?.currentBalance }
        }
        val rolledUpOpeningBalances = accountsByCode.keys.associateWith { code ->
            computeRolledUpAmount(code, accountsByCode) { rawOpeningBalances[it] }
        }
        return accounts.map { account -> buildFinalObject(account, accountsByCode, rolledUpBalances, rolledUpOpeningBalances) }
    }

    private fun computeRolledUpAmount(
        code: String,
        accountsByCode: Map<String, AccountDto>,
        leafAmount: (String) -> BigDecimal?
    ): BigDecimal {
        val children = accountsByCode.values.filter { it.parentAccountCode == code }
        return if (children.isEmpty()) {
            leafAmount(code) ?: BigDecimal.ZERO
        } else {
            children.fold(BigDecimal.ZERO) { acc, child ->
                acc + computeRolledUpAmount(child.code, accountsByCode, leafAmount)
            }
        }
    }

    private fun buildFinalObject(
        account: AccountDto,
        accountsByCode: Map<String, AccountDto>,
        rolledUpBalances: Map<String, BigDecimal>,
        rolledUpOpeningBalances: Map<String, BigDecimal>
    ): AccountResponseDto {
        val currentBalance = rolledUpBalances[account.code] ?: BigDecimal.ZERO
        val canGainChildren = canGainChildren(account, accountsByCode[account.parentAccountCode])
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
            accountIsExtensible = canGainChildren,
            balanceSignal = balanceSignal,
            openingBalance = rolledUpOpeningBalances[account.code] ?: BigDecimal.ZERO
        )
    }

    private fun canGainChildren(accountDto: AccountDto, parentAccountDto: AccountDto?): Boolean {
        if (accountDto.accountIsSystemMaintained) {
            return SystemAccount.fromCode(accountDto.code)?.isSingleLevelExtensionPoint() ?: false
        }
        // A single-level extension point grants exactly one level of children beneath it — this
        // account, sitting directly under one, is that one level and is not itself extensible.
        val parentSystemAccount = parentAccountDto?.let { SystemAccount.fromCode(it.code) }
        val parentIsSingleLevelExtensionPoint = parentSystemAccount?.isSingleLevelExtensionPoint() == true
        return !parentIsSingleLevelExtensionPoint
    }
}
