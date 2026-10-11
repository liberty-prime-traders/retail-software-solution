package me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.contact_opening_balance.api.ContactSubledgerDirection
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class ContactSubledgerDirectionConverter : EnumConverter<ContactSubledgerDirection>(ContactSubledgerDirection::class.java)
