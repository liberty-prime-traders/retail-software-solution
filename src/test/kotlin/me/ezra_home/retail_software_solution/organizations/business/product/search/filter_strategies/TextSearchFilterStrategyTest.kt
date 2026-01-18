package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.organizations.business.product.search.SearchMode
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextSearchFilterStrategyTest {

  @Test
  fun `FULLTEXT mode adds tsvector search`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("laptop", SearchMode.FULLTEXT).apply(context)

    assertTrue(context.whereClauses[0].contains("@@ plainto_tsquery"))
    assertEquals("laptop", context.params["nameOrDescription"])
  }

  @Test
  fun `TRIGRAM mode adds similarity search with OR for each field`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("phone", SearchMode.TRIGRAM).apply(context)

    val clause = context.whereClauses[0]
    assertTrue(clause.contains("LOWER(p.name) % LOWER(:nameOrDescription)"))
    assertTrue(clause.contains("OR LOWER(COALESCE(p.description, '')) % LOWER(:nameOrDescription)"))
    assertTrue(clause.contains("OR LOWER(COALESCE(p.product_group_name, '')) % LOWER(:nameOrDescription)"))
    assertEquals("phone", context.params["nameOrDescription"])
  }

  @Test
  fun `PREFIX mode adds LIKE with trailing percent`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("comp", SearchMode.PREFIX).apply(context)

    assertTrue(context.whereClauses[0].contains("LIKE LOWER(:nameOrDescription)"))
    assertEquals("comp%", context.params["nameOrDescription"])
  }

  @Test
  fun `WILDCARD mode adds LIKE with surrounding percents`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("tab", SearchMode.WILDCARD).apply(context)

    assertTrue(context.whereClauses[0].contains("COALESCE(p.description"))
    assertEquals("%tab%", context.params["nameOrDescription"])
  }

  @Test
  fun `null text ignored`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy(null, SearchMode.FULLTEXT).apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }

  @Test
  fun `blank text ignored`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("   ", SearchMode.FULLTEXT).apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }

  @Test
  fun `NONE mode ignores text`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("laptop", SearchMode.NONE).apply(context)

    assertTrue(context.whereClauses.isEmpty())
  }

  @Test
  fun `preserves special characters`() {
    val context = QueryBuilderContext()
    TextSearchFilterStrategy("laptop's & tablets", SearchMode.FULLTEXT).apply(context)

    assertEquals("laptop's & tablets", context.params["nameOrDescription"])
  }
}
