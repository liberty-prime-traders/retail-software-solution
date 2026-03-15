package me.ezra_home.retail_software_solution.cucumber.support

import com.google.common.collect.ArrayListMultimap
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
class InjectContext {

  private val store = ArrayListMultimap.create<String, String>()

  fun store(key: String, value: String) {
    store.put(key, value)
  }

  fun resolve(key: String, index: Int = 0): String? = store[key].getOrNull(index)

  fun inject(text: String): String {
    val matcher = PLACEHOLDER_PATTERN.matcher(text)
    val sb = StringBuffer()
    while (matcher.find()) {
      val placeholder = matcher.group(1)
      val parts = placeholder.split("->")
      val key = parts[0]
      val index = parts.getOrNull(1)?.toIntOrNull() ?: 0
      val resolved = resolve(key, index) ?: matcher.group()
      matcher.appendReplacement(sb, Regex.escapeReplacement(resolved))
    }
    matcher.appendTail(sb)
    return sb.toString()
  }

  fun clear() = store.clear()

  companion object {
    // matches #key or #key->index
    private val PLACEHOLDER_PATTERN = Pattern.compile("#([^\"\\s.!?,:;()\\[\\]{}/_]+)")
  }
}
