package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchValidator
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import kotlin.test.assertEquals

class ProductSearchValidatorTest {

  @Test
  fun `accepts arrays at max size of 50`() {
    val params = ProductSearchParameters(
      categoryIds = (1..50).map { UUID.randomUUID() }.toSet(),
      tagsIds = (1..50).map { UUID.randomUUID() }.toSet(),
      statusList = setOf(ProductStatus.ACTIVE, ProductStatus.DISCONTINUED, ProductStatus.AWAITING_FINAL_SALE)
    )

    assertDoesNotThrow {
      ProductSearchValidator.validateArraySizes(params.categoryIds, params.statusList, params.tagsIds)
    }
  }

  @Test
  fun `rejects categoryIds exceeding 50`() {
    val params = ProductSearchParameters(
      categoryIds = (1..51).map { UUID.randomUUID() }.toSet()
    )

    val exception = assertThrows<IllegalArgumentException> {
      ProductSearchValidator.validateArraySizes(params.categoryIds, params.statusList, params.tagsIds)
    }
    assertEquals(exception.message?.contains("categoryIds exceeds maximum size of 50"), true)
  }

  @Test
  fun `rejects tagIds exceeding 50`() {
    val params = ProductSearchParameters(
      tagsIds = (1..51).map { UUID.randomUUID() }.toSet()
    )

    val exception = assertThrows<IllegalArgumentException> {
      ProductSearchValidator.validateArraySizes(params.categoryIds, params.statusList, params.tagsIds)
    }
    assertEquals(exception.message?.contains("tagIds exceeds maximum size of 50"), true)
  }
}
