package me.ezra_home.retail_software_solution.business.address

import java.io.Serializable
import java.util.UUID

/**
 * DTO for {@link me.ezra_home.retail_software_solution.model.entity.AddressEntity}
 */
data class AddressDto(
    var id: UUID? = null,
    var line1: String? = null,
    var line2: String? = null,
    var line3: String? = null,
    var state: String? = null,
    var postalCode: String? = null,
    var country: String? = null
) : Serializable
