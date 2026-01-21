package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.SearchMode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryBuilderContextTest {

  @Test
  fun `accumulates filters from multiple strategies`() {
    val context = QueryBuilderContext()

    NameFilterStrategy("Product A").apply(context)
    StatusFilterStrategy(setOf("A")).apply(context)
    TextSearchFilterStrategy("laptop", SearchMode.FULLTEXT).apply(context)

    assertEquals(3, context.whereClauses.size)
    assertEquals(3, context.params.size)
  }

  @Test
  fun `supports both WHERE and HAVING clauses`() {
    val context = QueryBuilderContext()

    NameFilterStrategy("").apply(context)
    TagFilterStrategy(setOf(java.util.UUID.randomUUID())).apply(context)

    assertTrue(context.whereClauses.isNotEmpty())
    assertTrue(context.havingClauses.isNotEmpty())
  }

  @Test
  fun `initializes with empty collections`() {
    val context = QueryBuilderContext()

    assertTrue(context.whereClauses.isEmpty())
    assertTrue(context.havingClauses.isEmpty())
    assertTrue(context.params.isEmpty())
  }
}
