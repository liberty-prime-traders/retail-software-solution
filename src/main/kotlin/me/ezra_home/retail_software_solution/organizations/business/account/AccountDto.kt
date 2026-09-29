package me.ezra_home.retail_software_solution.organizations.business.account

import java.time.OffsetDateTime
import java.util.UUID

data class AccountDto(
    val id: UUID,
    val createdById: UUID,
    val createdOn: OffsetDateTime,
    val code: String,
    val name: String,
    val accountType: AccountType,
    val currencyCode: String,
    val accountIsActive: Boolean,
    val accountIsSystemMaintained: Boolean,
    val parentAccountCode: String?
) {
    val label: String get() = "$name (${toDisplayCode()})"
    fun toDisplayCode(): String {
        return code.split(".").joinToString(".") { it.trimStart('0').ifEmpty { "0" } }
    }

    fun applyTo(entity: AccountEntity): AccountEntity {
        return AccountEntity(
            code = entity.code,
            name = this.name,
            accountType = entity.accountType,
            currencyCode = entity.currencyCode,
            accountIsActive = this.accountIsActive,
            accountIsSystemMaintained = entity.accountIsSystemMaintained,
            parentAccountCode = entity.parentAccountCode
        ).apply {
            id = entity.id
            createdById = entity.createdById
            createdOn = entity.createdOn
        }
    }
}
