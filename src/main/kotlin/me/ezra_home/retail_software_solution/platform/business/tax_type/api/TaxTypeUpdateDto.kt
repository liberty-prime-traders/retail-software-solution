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
) : Serializable
