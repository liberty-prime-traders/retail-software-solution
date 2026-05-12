package me.ezra_home.retail_software_solution.cucumber.support.assertions

import org.springframework.stereotype.Component

@Component
class PersistedResponseRegistry(bindings: List<PersistedResponseBinding>) {
  private val bindingsByAlias = bindings.associateBy { it.alias }

  fun get(alias: String): PersistedResponseBinding =
    checkNotNull(bindingsByAlias[alias]) { "No persisted response binding registered for alias '$alias'" }
}
