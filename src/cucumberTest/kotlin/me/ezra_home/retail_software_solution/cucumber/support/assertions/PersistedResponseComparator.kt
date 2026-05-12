package me.ezra_home.retail_software_solution.cucumber.support.assertions

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.PersistentKey
import me.ezra_home.retail_software_solution.platform.business.organization.OrganizationCache
import org.springframework.stereotype.Component
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
    assertEquals(expectedNode, actualNode, "Response body did not match persisted '$alias' with id '$id'")
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
}
