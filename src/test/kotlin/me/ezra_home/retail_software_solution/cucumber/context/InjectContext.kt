package me.ezra_home.retail_software_solution.cucumber.context

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class InjectContext {

  private val multiStore = mutableMapOf<String, MutableList<String>>()
  private val persistentStore = mutableMapOf<String, MutableList<String>>()

  fun store(key: String, value: UUID) {
    multiStore.getOrPut(key) { mutableListOf() }.add(value.toString())
  }

  fun storeString(key: String, value: String) {
    multiStore.getOrPut(key) { mutableListOf() }.add(value)
  }

  fun persist(key: String, value: UUID) {
    persistentStore.getOrPut(key) { mutableListOf() }.add(value.toString())
  }

  fun persistString(key: String, value: String) {
    persistentStore.getOrPut(key) { mutableListOf() }.add(value)
  }

  fun find(key: String, index: Int? = null): UUID? = findString(key, index)?.let { UUID.fromString(it) }

  fun get(key: String, index: Int? = null): UUID = checkNotNull(find(key, index)) { "No value found in context for key '$key'" }

  fun findString(key: String, index: Int? = null): String? {
    persistentStore[key]?.let { list ->
      return if (index == null) list.lastOrNull() else list.getOrNull(index)
    }
    return multiStore[key]?.let { if (index == null) it.lastOrNull() else it.getOrNull(index) }
  }

  fun getString(key: String, index: Int? = null): String =
    checkNotNull(findString(key, index)) { "No value found in context for key '$key'" }

  fun inject(text: String): String =
    PLACEHOLDER_PATTERN.replace(text) { match ->
      val parts = match.groupValues[1].split("->")
      val key = parts[0]
      val index = parts.getOrNull(1)?.toIntOrNull()
      findString(key, index) ?: match.value
    }

  fun clear() = multiStore.clear()

  companion object {
    private val PLACEHOLDER_PATTERN = Regex("#([^\"\\s.!?,:;()\\[\\]{}/_]+)")
  }
}
