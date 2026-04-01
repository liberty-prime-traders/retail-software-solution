package me.ezra_home.retail_software_solution.cucumber.context

import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class InjectContext {

  private val store = mutableMapOf<String, MutableList<String>>()

  fun store(key: String, value: String) {
    store.getOrPut(key) { mutableListOf() }.add(value)
  }

  fun find(key: String, index: Int? = null): String? =
    store[key]?.let { if (index == null) it.lastOrNull() else it.getOrNull(index) }

  fun get(key: String, index: Int? = null): String =
    checkNotNull(find(key, index)) { "No value found in context for key '$key'" }

  fun inject(text: String): String {
    val matcher = PLACEHOLDER_PATTERN.matcher(text)
    val sb = StringBuffer()
    while (matcher.find()) {
      val placeholder = matcher.group(1)
      val parts = placeholder.split("->")
      val key = parts[0]
      val index = parts.getOrNull(1)?.toIntOrNull()
      val resolved = find(key, index) ?: matcher.group()
      matcher.appendReplacement(sb, Regex.escapeReplacement(resolved))
    }
    matcher.appendTail(sb)
    return sb.toString()
  }

  fun clear() = store.clear()

  companion object {
    private val PLACEHOLDER_PATTERN = Pattern.compile("#([^\"\\s.!?,:;()\\[\\]{}/_]+)")
  }
}
