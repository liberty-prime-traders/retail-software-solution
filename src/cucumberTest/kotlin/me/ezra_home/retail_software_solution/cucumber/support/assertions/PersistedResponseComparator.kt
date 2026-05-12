package me.ezra_home.retail_software_solution.cucumber.support.assertions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.PersistentKey
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

@Component
class PersistedResponseComparator(
  private val registry: PersistedResponseRegistry,
  private val injectContext: InjectContext,
  private val organizationCache: OrganizationCache,
  private val objectMapper: ObjectMapper,
) {

  fun assertBodyMatches(alias: String, actualNode: JsonNode, id: UUID) {
    val binding = registry.get(alias)
    val expectedNode = withScope(binding.scope) {
      objectMapper.valueToTree<JsonNode>(binding.responseDtoFor(id))
    }
    assertEquals(
      normalize(expectedNode.deepCopy()),
      normalize(actualNode.deepCopy()),
      "Response body did not match persisted '$alias' with id '$id'"
    )
  }

  private fun <T> withScope(scope: SchemaScope, block: () -> T): T {
    val sessionSnapshot = SessionContextProvider.getSession().copy()
    return try {
      if (scope == SchemaScope.ORGANIZATION) {
        val organizationId = injectContext.get(PersistentKey.ORGANIZATION)
        val organization = organizationCache.getAllOrganizations().find { it.id == organizationId }
          ?: error("Organization '$organizationId' not found in cache for cucumber assertion scope")
        SessionContextProvider.initOrganization(organization)
      }
      block()
    } finally {
      SessionContextProvider.setSession(sessionSnapshot)
    }
  }

  private fun normalize(node: JsonNode): JsonNode = when {
    node.isObject -> {
      val objectNode = node as ObjectNode
      objectNode.fieldNames().asSequence().toList().forEach { fieldName ->
        objectNode.replace(fieldName, normalize(objectNode.get(fieldName)))
      }
      objectNode
    }

    node.isArray -> {
      val arrayNode = node as ArrayNode
      for (index in 0 until arrayNode.size()) {
        arrayNode.set(index, normalize(arrayNode.get(index)))
      }
      arrayNode
    }

    node.isTextual -> normalizeDateTime(node)
    else -> node
  }

  private fun normalizeDateTime(node: JsonNode): JsonNode {
    val text = node.asText()
    return runCatching { TextNode.valueOf(OffsetDateTime.parse(text).toInstant().toString()) }
      .getOrElse { node }
  }
}
