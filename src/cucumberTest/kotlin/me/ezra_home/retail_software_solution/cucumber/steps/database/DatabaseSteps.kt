package me.ezra_home.retail_software_solution.cucumber.steps.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.database.DataAccessHelper
import me.ezra_home.retail_software_solution.cucumber.support.database.DataSourcePackage
import me.ezra_home.retail_software_solution.cucumber.support.rest.JsonSubsetMatcher
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Transactional
class DatabaseSteps(
  private val dataAccessHelper: DataAccessHelper,
  private val injectContext: InjectContext,
) {
  private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())

  @ParameterType(".*")
  fun dataSourcePackage(raw: String): DataSourcePackage = dataAccessHelper.get(raw)

  @ParameterType("should not|should")
  fun should(raw: String): Boolean = raw == "should"

  @ParameterType("\\S+")
  fun databaseId(raw: String): String = injectContext.inject(raw)

  @Then("{dataSourcePackage} {should} exist in database with id {databaseId}")
  fun checkIfExists(pkg: DataSourcePackage, should: Boolean, id: String) {
    val entity = pkg.findById(id)
    if (should)
      assertNotNull(entity, "Expected ${pkg.displayName} with id $id to exist")
    else
      assertNull(entity, "Expected no ${pkg.displayName} with id $id")
  }

  @Then("{dataSourcePackage} {should} exist in database with id {databaseId} and options:")
  fun checkDatabase(pkg: DataSourcePackage, should: Boolean, id: String, detail: String) {
    val entity = pkg.findById(id) ?: error("${pkg.displayName} with id $id not found in database")
    val expected = readTree(detail)
    val actual = objectMapper.valueToTree<JsonNode>(entity)

    val subject = "${pkg.displayName} row with id $id"
    if (should) {
      JsonSubsetMatcher.assertJsonSubset(expected, actual, subject = subject)
    } else {
      JsonSubsetMatcher.assertNotJsonSubset(expected, actual, subject = subject)
    }
  }

  @Then("{dataSourcePackage} {should} match example:")
  fun checkMatchExample(pkg: DataSourcePackage, should: Boolean, detail: String) {
    val expected = readTree(detail)
    val matched = pkg.repository.findAll().any { entity ->
      JsonSubsetMatcher.isJsonSubset(expected, objectMapper.valueToTree(entity))
    }
    if (should) {
      assertTrue(matched, "No ${pkg.displayName} row in database matches:\n$expected")
    } else {
      assertTrue(!matched, "A ${pkg.displayName} row in database unexpectedly matches:\n$expected")
    }
  }

  @Then("{dataSourcePackage} table should have exactly {int} records in database")
  fun checkRecordCount(pkg: DataSourcePackage, expectedCount: Int) {
    assertEquals(expectedCount.toLong(), pkg.repository.count(), "${pkg.displayName} record count mismatch")
  }

  @Then("{dataSourcePackage} table should have no records in database")
  fun checkEmpty(pkg: DataSourcePackage) {
    checkRecordCount(pkg, 0)
  }

  private fun DataSourcePackage.findById(rawId: String): Any? {
    val typedId = coerceId(rawId, idClass)
    return repository.findById(typedId).orElse(null)
  }

  private fun coerceId(raw: String, idClass: Class<*>): Any = when (idClass) {
    UUID::class.java -> UUID.fromString(raw)
    Long::class.javaObjectType -> raw.toLong()
    Int::class.javaObjectType -> raw.toInt()
    String::class.java -> raw
    else -> objectMapper.convertValue(raw, idClass)
  }

  private fun readTree(detail: String): JsonNode =
    objectMapper.readTree(injectContext.inject(detail))
}
