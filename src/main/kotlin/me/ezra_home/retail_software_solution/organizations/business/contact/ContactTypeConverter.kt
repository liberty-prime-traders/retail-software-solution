package me.ezra_home.retail_software_solution.organizations.business.contact

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.enums.EnumSetConverter

@Converter(autoApply = true)
class ContactTypeConverter : EnumSetConverter<ContactType>(ContactType::class.java)
