package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CursorFilterStrategyTest {

  @Test
  fun `applies cursor filter correctly`() {
    val context = QueryBuilderContext()
    CursorFilterStrategy(12345L).apply(context)

    assertTrue(context.whereClauses[0].contains("p.cursor > :previousCursor"))
    assertEquals(12345L, context.params["previousCursor"])
  }

  @Test
  fun `handles zero cursor value`() {
    val context = QueryBuilderContext()
    CursorFilterStrategy(0L).apply(context)

    assertEquals(0L, context.params["previousCursor"])
  }

  @Test
  fun `handles max long value`() {
    val context = QueryBuilderContext()
    CursorFilterStrategy(Long.MAX_VALUE).apply(context)

    assertEquals(Long.MAX_VALUE, context.params["previousCursor"])
  }
}
