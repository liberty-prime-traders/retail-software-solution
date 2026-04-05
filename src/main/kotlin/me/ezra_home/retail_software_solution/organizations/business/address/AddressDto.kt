package me.ezra_home.retail_software_solution.organizations.business.address

import java.time.OffsetDateTime
import java.util.UUID

data class AddressDto(
    var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var line1: String? = null,
    var line2: String? = null,
    var line3: String? = null,
    var state: String? = null,
    var postalCode: String? = null,
    var country: String? = null
)
