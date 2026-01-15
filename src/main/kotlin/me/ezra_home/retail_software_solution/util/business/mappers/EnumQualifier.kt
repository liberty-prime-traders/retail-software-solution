package me.ezra_home.retail_software_solution.util.business.mappers

import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import me.ezra_home.retail_software_solution.util.enums.ProductStatusConverter
import org.springframework.stereotype.Component

@Component
object EnumQualifier {
  fun toProductStatus(code: String?): ProductStatus? {
    return ProductStatusConverter().convertToEntityAttribute(code)
  }
}
