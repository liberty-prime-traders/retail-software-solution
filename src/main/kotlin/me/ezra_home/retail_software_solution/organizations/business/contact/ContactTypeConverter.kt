package me.ezra_home.retail_software_solution.organizations.business.contact

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
internal class ContactTypeConverter : EnumConverter<ContactType>(ContactType::class.java)
