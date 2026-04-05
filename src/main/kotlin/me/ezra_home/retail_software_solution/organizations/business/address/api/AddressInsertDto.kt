package me.ezra_home.retail_software_solution.organizations.business.address.api

import java.io.Serializable

data class AddressInsertDto(
    val line1: String? = null,
    val line2: String? = null,
    val line3: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
) : Serializable
