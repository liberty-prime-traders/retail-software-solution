package me.ezra_home.retail_software_solution.organizations.business.address.api

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
) : Serializable
