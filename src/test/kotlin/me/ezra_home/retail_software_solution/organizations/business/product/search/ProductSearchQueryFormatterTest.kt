package me.ezra_home.retail_software_solution.organizations.business.product.search

import me.ezra_home.retail_software_solution.organizations.business.product.search.TestDataFactory.TestUUIDs
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductSearchQueryFormatterTest {

  @Test
  fun `formats string parameters with quotes`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = "SELECT * FROM product WHERE name = :productName",
      params = mapOf("productName" to "laptop's & tablets"),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10)

    assertFalse(result.contains(":productName"))
    assertTrue(result.contains("'laptop's & tablets'"))
  }

  @Test
  fun `formats UUID parameters with quotes`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = "SELECT * FROM product WHERE id = :productId",
      params = mapOf("productId" to TestUUIDs.UUID1),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10)

    assertTrue(result.contains("'${TestUUIDs.UUID1}'"))
  }

  @Test
  fun `formats array parameters`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = "SELECT * FROM product WHERE id = ANY(:ids) AND status = ANY(:statusList)",
      params = mapOf(
        "ids" to arrayOf(TestUUIDs.UUID1, TestUUIDs.UUID2),
        "statusList" to arrayOf("A", "X")
      ),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10)

    assertTrue(result.contains("ARRAY['${TestUUIDs.UUID1}', '${TestUUIDs.UUID2}']"))
    assertTrue(result.contains("ARRAY['A', 'X']"))
  }

  @Test
  fun `formats number parameters without quotes`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = "SELECT * FROM product WHERE cursor > :previousCursor AND count = :tagCount",
      params = mapOf("previousCursor" to 12345L, "tagCount" to 2),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10)

    assertTrue(result.contains("12345"))
    assertTrue(result.contains("2"))
  }

  @Test
  fun `adds pageSize parameter`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = "SELECT * FROM product LIMIT :pageSize",
      params = emptyMap(),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 25)

    assertTrue(result.contains("25"))
    assertFalse(result.contains(":pageSize"))
  }

  @Test
  fun `handles parameters with similar names`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = "SELECT * FROM product WHERE name = :name AND full_name = :fullName",
      params = mapOf("name" to "laptop", "fullName" to "laptop computer"),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 10)

    assertTrue(result.contains("'laptop computer'"))
    assertTrue(result.contains("'laptop'"))
    assertFalse(result.contains(":name"))
  }

  @Test
  fun `formats complex subquery with all parameter types`() {
    val sqlQuery = ProductSearchUtilityTypes.SqlQuery(
      sql = """
        SELECT p.*
        FROM (
          SELECT p.id WHERE p.cursor > :previousCursor
          AND p.status = ANY(:statusList)
          AND p.name LIKE :searchText
          AND pt.tag_id = ANY(:tagIds)
          HAVING COUNT(DISTINCT pt.tag_id) = :tagIdsCount
          LIMIT :pageSize
        ) final_ids
      """.trimIndent(),
      params = mapOf(
        "previousCursor" to 500L,
        "statusList" to arrayOf("A"),
        "searchText" to "%laptop%",
        "tagIds" to arrayOf(TestUUIDs.UUID1, TestUUIDs.UUID2),
        "tagIdsCount" to 2
      ),
      metadata = ProductSearchUtilityTypes.QueryMetadata()
    )

    val result = ProductSearchQueryFormatter.formatQueryWithParameters(sqlQuery, pageSize = 100)

    assertFalse(result.contains(":previousCursor"))
    assertFalse(result.contains(":statusList"))
    assertFalse(result.contains(":searchText"))
    assertFalse(result.contains(":tagIds"))
    assertFalse(result.contains(":tagIdsCount"))
    assertFalse(result.contains(":pageSize"))
    assertTrue(result.contains("500"))
    assertTrue(result.contains("'%laptop%'"))
    assertTrue(result.contains("100"))
  }
}
