package me.ezra_home.retail_software_solution.business.address

import me.ezra_home.retail_software_solution.business.audit.AuditDto
import java.io.Serializable

data class AddressWithAudit(
    val audit: AuditDto?,
    val address: AddressDto
): Serializable
