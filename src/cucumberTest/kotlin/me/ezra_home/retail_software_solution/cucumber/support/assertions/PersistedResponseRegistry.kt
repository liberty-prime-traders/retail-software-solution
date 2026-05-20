package me.ezra_home.retail_software_solution.cucumber.support.assertions

import org.springframework.stereotype.Component

@Component
class PersistedResponseRegistry(bindings: List<PersistedResponseBinding>) {
  private val bindingsByAlias = bindings
    .also { registeredBindings ->
      val duplicateAliases = registeredBindings.groupBy { it.alias }.filterValues { it.size > 1 }.keys.sorted()
      require(duplicateAliases.isEmpty()) { "Duplicate persisted response binding aliases registered: ${duplicateAliases.joinToString(", ")}" }
    }
    .associateBy { it.alias }

  fun get(alias: String): PersistedResponseBinding =
    checkNotNull(bindingsByAlias[alias]) { "No persisted response binding registered for alias '$alias'" }
}
