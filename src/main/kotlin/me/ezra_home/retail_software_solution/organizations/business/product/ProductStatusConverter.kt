package me.ezra_home.retail_software_solution.organizations.business.product

import jakarta.persistence.Converter
import me.ezra_home.retail_software_solution.organizations.business.product.public.ProductStatus
import me.ezra_home.retail_software_solution.util.enums.EnumConverter

@Converter(autoApply = true)
class ProductStatusConverter : EnumConverter<ProductStatus>(ProductStatus::class.java)
