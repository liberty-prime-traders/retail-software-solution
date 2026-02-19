package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.util.queries.SearchStrategy
import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ProductSearchParameters
import me.ezra_home.retail_software_solution.cross_tier.product.search.organization.OrganizationProductQueryBuilder
import me.ezra_home.retail_software_solution.organizations.business.product.search.TestDataFactory.TestUUIDs
import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BuilderTest {

  @Test
  fun `minimal query with name and status`() {
    val params = ProductSearchParameters(statusList = setOf(ProductStatus.ACTIVE))
    val result = OrganizationProductQueryBuilder.buildSearchQuery(params, previousName = "Product A")

    assertTrue(result.sql.contains("SELECT p.*"))
    assertTrue(result.sql.contains("FROM product p"))
    assertTrue(result.sql.contains("LOWER(p.name) > LOWER(:previousName)"))
    assertTrue(result.sql.contains("p.status = ANY(:statusList)"))
    assertFalse(result.sql.contains("GROUP BY"))
    assertEquals("Product A", result.params["previousName"])
    assertTrue((result.params["statusList"] as Array<*>).contentEquals(arrayOf("A")))
  }

  @Test
  fun `text search adds correct clause per mode`() {
    val fulltext = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(searchText = "laptop", searchStrategy = SearchStrategy.FULLTEXT),
      previousName = ""
    )
    val trigram = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(searchText = "pakaging", searchStrategy = SearchStrategy.TRIGRAM),
      previousName = ""
    )
    val prefix = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(searchText = "comp", searchStrategy = SearchStrategy.PREFIX),
      previousName = ""
    )
    val wildcard = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(searchText = "tab", searchStrategy = SearchStrategy.WILDCARD),
      previousName = ""
    )

    assertTrue(fulltext.sql.contains("p.search_vector @@ plainto_tsquery"))
    assertEquals("laptop", fulltext.params["nameOrDescription"])

    assertTrue(trigram.sql.contains("LOWER(p.name) % LOWER(:nameOrDescription)"))
    assertTrue(trigram.sql.contains("OR LOWER(p.product_group_name) % LOWER(:nameOrDescription)"))
    assertTrue(trigram.sql.contains("OR LOWER(p.description) % LOWER(:nameOrDescription)"))
    assertEquals("pakaging", trigram.params["nameOrDescription"])

    assertTrue(prefix.sql.contains("LOWER(p.name) LIKE LOWER(:nameOrDescription)"))
    assertEquals("comp%", prefix.params["nameOrDescription"])

    assertTrue(wildcard.sql.contains("LOWER(p.name) LIKE LOWER(:nameOrDescription)"))
    assertTrue(wildcard.sql.contains("OR LOWER(p.product_group_name) LIKE LOWER(:nameOrDescription)"))
    assertTrue(wildcard.sql.contains("OR LOWER(p.description) LIKE LOWER(:nameOrDescription)"))
    assertEquals("%tab%", wildcard.params["nameOrDescription"])
  }

  @Test
  fun `blank or NONE search mode ignored`() {
    val blank = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(searchText = "   ", searchStrategy = SearchStrategy.FULLTEXT),
      previousName = ""
    )
    val none = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(searchText = "laptop", searchStrategy = SearchStrategy.NONE),
      previousName = ""
    )

    assertFalse(blank.params.containsKey("nameOrDescription"))
    assertFalse(none.params.containsKey("nameOrDescription"))
  }

  @Test
  fun `category filter adds product_group join`() {
    val result = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(categoryIds = setOf(TestUUIDs.UUID1, TestUUIDs.UUID2)),
      previousName = ""
    )

    assertTrue(result.sql.contains("INNER JOIN product_group pg ON p.product_group_id = pg.id"))
    assertTrue(result.sql.contains("pg.category_id = ANY(:categoryIds)"))
    assertEquals(2, (result.params["categoryIds"] as Array<*>).size)
  }

  @Test
  fun `tag filter triggers subquery pattern`() {
    val result = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(tagIds = setOf(TestUUIDs.UUID1, TestUUIDs.UUID2)),
      previousName = ""
    )

    assertTrue(result.sql.contains("SELECT p.*"))
    assertTrue(result.sql.contains("FROM ("))
    assertTrue(result.sql.contains("filtered_products.id"))
    assertTrue(result.sql.contains("INNER JOIN product_tag pt"))
    assertTrue(result.sql.contains("GROUP BY"))
    assertTrue(result.sql.contains("HAVING COUNT(DISTINCT pt.tag_id) = :tagIdsCount"))
    assertTrue(result.sql.contains("pt.end_on IS NULL"))
    assertEquals(2, result.params["tagIdsCount"])
  }

  @Test
  fun `tag filter with category includes product_group join in subquery`() {
    val result = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(
        tagIds = setOf(TestUUIDs.UUID1),
        categoryIds = setOf(TestUUIDs.UUID2)
      ),
      previousName = ""
    )

    assertTrue(result.sql.contains("INNER JOIN product_group pg"))
    assertTrue(result.sql.contains("INNER JOIN product_tag pt"))
  }

  @Test
  fun `combined filters produce correct metadata`() {
    val result = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(
        searchText = "laptop",
        searchStrategy = SearchStrategy.FULLTEXT,
        referenceNumber = "REF",
        categoryIds = setOf(TestUUIDs.UUID1, TestUUIDs.UUID2),
        tagIds = setOf(TestUUIDs.UUID3),
        statusList = setOf(ProductStatus.ACTIVE, ProductStatus.DISCONTINUED)
      ),
      previousName = ""
    )

    assertEquals(2, result.metadata.categoryIdsCount)
    assertEquals(1, result.metadata.tagIdsCount)
    assertEquals(2, result.metadata.statusListCount)
    assertTrue(result.metadata.hasTextSearch)
    assertTrue(result.metadata.hasReferenceNumberSearch!!)
    assertTrue(result.metadata.hasTagFilter!!)
  }

  @Test
  fun `multiple status codes formatted correctly`() {
    val result = OrganizationProductQueryBuilder.buildSearchQuery(
      ProductSearchParameters(
        statusList = setOf(ProductStatus.ACTIVE, ProductStatus.DISCONTINUED, ProductStatus.AWAITING_FINAL_SALE)
      ),
      previousName = ""
    )

    val statusArray = result.params["statusList"] as Array<*>
    assertEquals(3, statusArray.size)
    assertTrue(statusArray.contains("A"))
    assertTrue(statusArray.contains("X"))
    assertTrue(statusArray.contains("AFS"))
  }
}
