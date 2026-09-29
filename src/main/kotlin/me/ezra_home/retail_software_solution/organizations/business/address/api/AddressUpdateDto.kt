package me.ezra_home.retail_software_solution.organizations.business.address.api

import me.ezra_home.retail_software_solution.organizations.business.address.AddressDto
import java.io.Serializable
import java.util.Optional
import java.util.UUID

data class AddressUpdateDto(
    val id: UUID? = null,
    val line1: Optional<String>? = null,
    val line2: Optional<String>? = null,
    val line3: Optional<String>? = null,
    val state: Optional<String>? = null,
    val postalCode: Optional<String>? = null,
    val country: Optional<String>? = null
) : Serializable {

    fun applyTo(existing: AddressDto): AddressDto = existing.copy(
        line1 = line1?.orElse(existing.line1) ?: existing.line1,
        line2 = line2?.orElse(existing.line2) ?: existing.line2,
        line3 = line3?.orElse(existing.line3) ?: existing.line3,
        state = state?.orElse(existing.state) ?: existing.state,
        postalCode = postalCode?.orElse(existing.postalCode) ?: existing.postalCode,
        country = country?.orElse(existing.country) ?: existing.country
    )
}
