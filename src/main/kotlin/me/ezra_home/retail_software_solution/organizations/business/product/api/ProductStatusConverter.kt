package me.ezra_home.retail_software_solution.organizations.business.product.api

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class ProductStatusConverter : EnumConverter<ProductStatus>(ProductStatus::class.java)
