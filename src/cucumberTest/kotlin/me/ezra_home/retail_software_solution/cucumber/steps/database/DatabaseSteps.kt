package me.ezra_home.retail_software_solution.cucumber.steps.database

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.cucumber.java.ParameterType
import io.cucumber.java.en.Then
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.configuration.session.OrgSession
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.PersistentKey
import me.ezra_home.retail_software_solution.cucumber.support.database.DataAccessHelper
import me.ezra_home.retail_software_solution.cucumber.support.database.DataSourcePackage
import me.ezra_home.retail_software_solution.cucumber.support.database.Schema
import me.ezra_home.retail_software_solution.cucumber.support.rest.JsonSubsetMatcher
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationRepository
import org.springframework.context.ApplicationContext
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseSteps(
  private val dataAccessHelper: DataAccessHelper,
  private val injectContext: InjectContext,
  private val applicationContext: ApplicationContext,
  private val organizationRepository: OrganizationRepository,
) {
  private val objectMapper: ObjectMapper = jacksonObjectMapper()
    .registerKotlinModule()
    .registerModule(JavaTimeModule())

  private val transactionManagers by lazy {
    mapOf(
      Schema.PLATFORM to applicationContext.getBean(
        DataSourceBeanNames.PLATFORM_SCHEMA_TRANSACTION_MANAGER,
        PlatformTransactionManager::class.java
      ),
      Schema.ORGANIZATION to applicationContext.getBean(
        DataSourceBeanNames.ORGANIZATION_SCHEMA_TRANSACTION_MANAGER,
        PlatformTransactionManager::class.java
      ),
      Schema.LOCATION to applicationContext.getBean(
        DataSourceBeanNames.LOCATION_SCHEMA_TRANSACTION_MANAGER,
        PlatformTransactionManager::class.java
      )
    )
  }

  private fun <T> executeInTransaction(schema: Schema, action: () -> T): T? {
    val txManager = transactionManagers[schema]
      ?: error("No transaction manager found for schema: $schema")
    
    // Initialize SessionContext for organization and location schemas
    if (schema == Schema.ORGANIZATION || schema == Schema.LOCATION) {
      initializeSessionContext(schema)
    }
    
    return try {
      TransactionTemplate(txManager).execute { action() }
    } finally {
      // Clear SessionContext after query to avoid side effects
      if (schema == Schema.ORGANIZATION || schema == Schema.LOCATION) {
        SessionContextProvider.clear()
      }
    }
  }

  private fun <T : Any> executeInTransactionNonNull(schema: Schema, action: () -> T): T {
    return executeInTransaction(schema, action)
      ?: error("Transaction callback unexpectedly returned null")
  }

  private fun initializeSessionContext(schema: Schema) {
    when (schema) {
      Schema.ORGANIZATION -> {
        val orgId = injectContext.find(PersistentKey.ORGANIZATION)
          ?: error("Organization ID not found in context. Ensure test is authenticated as organization user.")
        
        // Load organization details using platform transaction
        val platformTxManager = transactionManagers[Schema.PLATFORM]!!
        val org = TransactionTemplate(platformTxManager).execute {
          organizationRepository.findById(orgId).orElseThrow {
            error("Organization with id $orgId not found in database")
          }
        } ?: error("Failed to load organization")
        
        // Initialize SessionContext with organization using OrgSession
        SessionContextProvider.getSession().organization = OrgSession(
          id = org.id!!,
          schemaName = org.schemaName!!,
          timezone = org.timezone
        )
      }
      Schema.LOCATION -> {
        injectContext.find(PersistentKey.LOCATION)
          ?: error("Location ID not found in context. Ensure test has location context set.")
        
        // For location schema, we need both organization and location context
        // This would require loading location details - implement when needed
        error("Location schema database assertions not yet implemented")
      }
      Schema.PLATFORM -> {
        // Platform schema doesn't require SessionContext
      }
    }
  }

  @ParameterType(".*")
  fun dataSourcePackage(raw: String): DataSourcePackage = dataAccessHelper.get(raw)

  @ParameterType("should not|should")
  fun should(raw: String): Boolean = raw == "should"

  @ParameterType("\\S+")
  fun databaseId(raw: String): String = injectContext.inject(raw)

  @Then("{dataSourcePackage} {should} exist in database with id {databaseId}")
  fun checkIfExists(pkg: DataSourcePackage, should: Boolean, id: String) {
    val entity = executeInTransaction(pkg.schema) { pkg.findById(id) }
    if (should)
      assertNotNull(entity, "Expected ${pkg.displayName} with id $id to exist")
    else
      assertNull(entity, "Expected no ${pkg.displayName} with id $id")
  }

  @Then("{dataSourcePackage} {should} exist in database with id {databaseId} and options:")
  fun checkDatabase(pkg: DataSourcePackage, should: Boolean, id: String, detail: String) {
    val entity = executeInTransaction(pkg.schema) {
      pkg.findById(id)
    }
    
    // Handle negative assertion: entity should NOT exist with these options
    if (!should) {
      if (entity == null) {
        // Entity doesn't exist, negative assertion passes (entity can't match options if it doesn't exist)
        return
      }
      // Entity exists, check if it does NOT match the options
      val expected = readTree(detail)
      val actual = objectMapper.valueToTree<JsonNode>(entity)
      val subject = "${pkg.displayName} row with id $id"
      JsonSubsetMatcher.assertNotJsonSubset(expected, actual, subject = subject)
      return
    }
    
    // Handle positive assertion: entity SHOULD exist with these options
    if (entity == null) {
      error("${pkg.displayName} with id $id not found in database")
    }
    
    val expected = readTree(detail)
    val actual = objectMapper.valueToTree<JsonNode>(entity)
    val subject = "${pkg.displayName} row with id $id"
    JsonSubsetMatcher.assertJsonSubset(expected, actual, subject = subject)
  }

  @Then("{dataSourcePackage} {should} match example:")
  fun checkMatchExample(pkg: DataSourcePackage, should: Boolean, detail: String) {
    val expected = readTree(detail)
    val matched = executeInTransactionNonNull(pkg.schema) {
      pkg.repository.findAll().any { entity ->
        JsonSubsetMatcher.isJsonSubset(expected, objectMapper.valueToTree(entity))
      }
    }
    if (should) {
      assertTrue(matched, "No ${pkg.displayName} row in database matches:\n$expected")
    } else {
      assertTrue(!matched, "A ${pkg.displayName} row in database unexpectedly matches:\n$expected")
    }
  }

  @Then("{dataSourcePackage} table should have exactly {int} records in database")
  fun checkRecordCount(pkg: DataSourcePackage, expectedCount: Int) {
    val actualCount = executeInTransactionNonNull(pkg.schema) { pkg.repository.count() }
    assertEquals(expectedCount.toLong(), actualCount, "${pkg.displayName} record count mismatch")
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
