package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

import me.ezra_home.retail_software_solution.util.queries.QueryBuilderContext
import me.ezra_home.retail_software_solution.cross_tier.product.search.organization.filters.TagFilterStrategy
import me.ezra_home.retail_software_solution.organizations.business.product.search.TestDataFactory.TestUUIDs
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TagFilterStrategyTest {

  @Test
  fun `applies WHERE and HAVING clauses for tag intersection`() {
    val context = QueryBuilderContext()
    TagFilterStrategy(setOf(TestUUIDs.UUID1, TestUUIDs.UUID2)).apply(context)

    assertEquals(2, context.whereClauses.size)
    assertTrue(context.whereClauses.any { it.contains("pt.tag_id = ANY(:tagIds)") })
    assertTrue(context.whereClauses.any { it.contains("pt.end_on IS NULL") })
    assertTrue(context.havingClauses[0].contains("COUNT(DISTINCT pt.tag_id) = :tagIdsCount"))
    assertEquals(2, context.params["tagIdsCount"])
  }

  @Test
  fun `filters only active tags with end_on IS NULL`() {
    val context = QueryBuilderContext()
    TagFilterStrategy(setOf(TestUUIDs.UUID1)).apply(context)

    assertTrue(context.whereClauses.any { it.contains("pt.end_on IS NULL") })
  }

  @Test
  fun `empty tag set adds no clauses`() {
    val context = QueryBuilderContext()
    TagFilterStrategy(emptySet()).apply(context)

    assertTrue(context.whereClauses.isEmpty())
    assertTrue(context.havingClauses.isEmpty())
  }

  @Test
  fun `tagIdsCount matches number of tags for intersection logic`() {
    val context = QueryBuilderContext()
    TagFilterStrategy(setOf(TestUUIDs.UUID1, TestUUIDs.UUID2, TestUUIDs.UUID3)).apply(context)

    assertEquals(3, context.params["tagIdsCount"])
    assertEquals(3, (context.params["tagIds"] as Array<*>).size)
  }
}
