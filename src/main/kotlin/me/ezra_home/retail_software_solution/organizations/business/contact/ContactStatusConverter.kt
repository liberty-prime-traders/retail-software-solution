package me.ezra_home.retail_software_solution.organizations.business.contact

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class ContactStatusConverter : EnumConverter<ContactStatus>(ContactStatus::class.java)
