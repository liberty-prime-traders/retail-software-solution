package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.cross_tier.product.search.common.ParameterNames
import me.ezra_home.retail_software_solution.util.queries.QueryFormatter
import me.ezra_home.retail_software_solution.util.queries.SqlQuery
import me.ezra_home.retail_software_solution.util.queries.QueryMetadata
import me.ezra_home.retail_software_solution.organizations.business.product.search.TestDataFactory.TestUUIDs
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductSearchQueryFormatterTest {

  @Test
  fun `formats string parameters with quotes`() {
    val sqlQuery = SqlQuery(
      sql = "SELECT * FROM product WHERE name = :productName",
      params = mapOf("productName" to "laptop's & tablets"),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10, ParameterNames.PAGE_SIZE)

    assertFalse(result.contains(":productName"))
    assertTrue(result.contains("'laptop's & tablets'"))
  }

  @Test
  fun `formats UUID parameters with quotes`() {
    val sqlQuery = SqlQuery(
      sql = "SELECT * FROM product WHERE id = :productId",
      params = mapOf("productId" to TestUUIDs.UUID1),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10, ParameterNames.PAGE_SIZE)

    assertTrue(result.contains("'${TestUUIDs.UUID1}'"))
  }

  @Test
  fun `formats array parameters`() {
    val sqlQuery = SqlQuery(
      sql = "SELECT * FROM product WHERE id = ANY(:ids) AND status = ANY(:statusList)",
      params = mapOf(
        "ids" to arrayOf(TestUUIDs.UUID1, TestUUIDs.UUID2),
        "statusList" to arrayOf("A", "X")
      ),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10, ParameterNames.PAGE_SIZE)

    assertTrue(result.contains("ARRAY['${TestUUIDs.UUID1}', '${TestUUIDs.UUID2}']"))
    assertTrue(result.contains("ARRAY['A', 'X']"))
  }

  @Test
  fun `formats number parameters without quotes`() {
    val sqlQuery = SqlQuery(
      sql = "SELECT * FROM product WHERE count = :tagCount AND price > :minPrice",
      params = mapOf("tagCount" to 2, "minPrice" to 12345L),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10, ParameterNames.PAGE_SIZE)

    assertTrue(result.contains("12345"))
    assertTrue(result.contains("2"))
  }

  @Test
  fun `adds pageSize parameter`() {
    val sqlQuery = SqlQuery(
      sql = "SELECT * FROM product LIMIT :pageSize",
      params = emptyMap(),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 25, ParameterNames.PAGE_SIZE)

    assertTrue(result.contains("25"))
    assertFalse(result.contains(":pageSize"))
  }

  @Test
  fun `handles parameters with similar names`() {
    val sqlQuery = SqlQuery(
      sql = "SELECT * FROM product WHERE name = :name AND full_name = :fullName",
      params = mapOf("name" to "laptop", "fullName" to "laptop computer"),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10, ParameterNames.PAGE_SIZE)

    assertTrue(result.contains("'laptop computer'"))
    assertTrue(result.contains("'laptop'"))
    assertFalse(result.contains(":name"))
  }

  @Test
  fun `formats complex subquery with all parameter types`() {
    val sqlQuery = SqlQuery(
      sql = """
        SELECT p.*
        FROM (
          SELECT p.id WHERE LOWER(p.name) > LOWER(:previousName)
          AND p.status = ANY(:statusList)
          AND p.name LIKE :searchText
          AND pt.tag_id = ANY(:tagIds)
          HAVING COUNT(DISTINCT pt.tag_id) = :tagIdsCount
          LIMIT :pageSize
        ) final_ids
      """.trimIndent(),
      params = mapOf(
        "previousName" to "Product A",
        "statusList" to arrayOf("A"),
        "searchText" to "%laptop%",
        "tagIds" to arrayOf(TestUUIDs.UUID1, TestUUIDs.UUID2),
        "tagIdsCount" to 2
      ),
      metadata = QueryMetadata()
    )

    val result = QueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 100, ParameterNames.PAGE_SIZE)

    assertFalse(result.contains(":previousName"))
    assertFalse(result.contains(":statusList"))
    assertFalse(result.contains(":searchText"))
    assertFalse(result.contains(":tagIds"))
    assertFalse(result.contains(":tagIdsCount"))
    assertFalse(result.contains(":pageSize"))
    assertTrue(result.contains("'Product A'"))
    assertTrue(result.contains("'%laptop%'"))
    assertTrue(result.contains("100"))
  }
}
