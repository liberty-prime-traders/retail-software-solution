package me.ezra_home.retail_software_solution.platform.business.tax_type.api

import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class TaxTypeUpdateDto(
    val id: UUID,
    val name: Optional<String>? = null,
    val description: Optional<String>? = null,
    val taxRecoveryType: Optional<TaxRecoveryType>? = null,
    val taxApplicationLevel: Optional<TaxApplicationLevel>? = null,
    val taxTriggers: Optional<List<TaxTrigger>>? = null
) : Serializable {

    fun applyTo(existing: TaxTypeDto): TaxTypeDto = existing.copy(
        name = name?.orElse(existing.name) ?: existing.name,
        description = description?.orElse(existing.description) ?: existing.description,
        taxRecoveryType = taxRecoveryType?.orElse(existing.taxRecoveryType) ?: existing.taxRecoveryType,
        taxApplicationLevel = taxApplicationLevel?.orElse(existing.taxApplicationLevel) ?: existing.taxApplicationLevel,
        taxTriggers = taxTriggers?.orElse(existing.taxTriggers) ?: existing.taxTriggers
    )
}
