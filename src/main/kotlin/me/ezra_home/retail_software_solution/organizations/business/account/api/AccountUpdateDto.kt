package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.organizations.business.account.AccountDto
import java.util.Optional
import java.util.UUID

data class AccountUpdateDto(
    val id: UUID,
    val name: Optional<String>
) {

    fun applyTo(existing: AccountDto): AccountDto {
        return existing.copy(
            name = name.orElseGet { existing.name }
        )
    }
}
