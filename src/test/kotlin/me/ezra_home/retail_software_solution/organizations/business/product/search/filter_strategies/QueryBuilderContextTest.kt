package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.NameFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.StatusFilterStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.filters.TextSearchFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.TagFilterStrategy
import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.util.queries.SearchStrategy
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QueryBuilderContextTest {

  @Test
  fun `accumulates filters from multiple strategies`() {
    val context = QueryBuilderContext()

    NameFilterStrategy("Product A").apply(context)
    StatusFilterStrategy(setOf("A")).apply(context)
    TextSearchFilterStrategy("laptop", SearchStrategy.FULLTEXT).apply(context)

    assertEquals(3, context.whereClauses.size)
    assertEquals(3, context.params.size)
  }

  @Test
  fun `supports both WHERE and HAVING clauses`() {
    val context = QueryBuilderContext()

    NameFilterStrategy("").apply(context)
    TagFilterStrategy(setOf(UUID.randomUUID())).apply(context)

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
