package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.TestDataFactory.TestUUIDs
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CategoryFilterStrategyTest {

  @Test
  fun `applies single category UUID`() {
    val context = QueryBuilderContext()
    CategoryFilterStrategy(setOf(TestUUIDs.UUID1)).apply(context)

    assertTrue(context.whereClauses[0].contains("pg.category_id = ANY(:categoryIds)"))
    assertEquals(1, (context.params["categoryIds"] as Array<*>).size)
  }

  @Test
  fun `applies multiple category UUIDs`() {
    val context = QueryBuilderContext()
    CategoryFilterStrategy(setOf(TestUUIDs.UUID1, TestUUIDs.UUID2, TestUUIDs.UUID3)).apply(context)

    val categoryArray = context.params["categoryIds"] as Array<*>
    assertEquals(3, categoryArray.size)
    assertTrue(categoryArray.contains(TestUUIDs.UUID1))
    assertTrue(categoryArray.contains(TestUUIDs.UUID2))
    assertTrue(categoryArray.contains(TestUUIDs.UUID3))
  }

  @Test
  fun `empty category set adds no filter`() {
    val context = QueryBuilderContext()
    CategoryFilterStrategy(emptySet()).apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }
}
