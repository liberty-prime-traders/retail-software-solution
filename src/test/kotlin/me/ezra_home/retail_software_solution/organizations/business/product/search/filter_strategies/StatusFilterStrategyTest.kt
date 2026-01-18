package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatusFilterStrategyTest {

  @Test
  fun `applies single status code`() {
    val context = QueryBuilderContext()
    StatusFilterStrategy(setOf("A")).apply(context)

    assertTrue(context.whereClauses[0].contains("p.status = ANY(:statusList)"))
    assertTrue((context.params["statusList"] as Array<*>).contentEquals(arrayOf("A")))
  }

  @Test
  fun `applies multiple status codes`() {
    val context = QueryBuilderContext()
    StatusFilterStrategy(setOf("A", "X", "AFS")).apply(context)

    val statusArray = context.params["statusList"] as Array<*>
    assertEquals(3, statusArray.size)
    assertTrue(statusArray.contains("A"))
    assertTrue(statusArray.contains("X"))
    assertTrue(statusArray.contains("AFS"))
  }

  @Test
  fun `empty status set adds no filter`() {
    val context = QueryBuilderContext()
    StatusFilterStrategy(emptySet()).apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }
}
