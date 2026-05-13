package me.ezra_home.retail_software_solution.cucumber.support.rest

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.math.RoundingMode

data class SubsetOptions(
  val exactLists: Boolean = false,
  val inOrder: Boolean = false,
)

/**
 * Subset-matches `expected` against `actual`:
 * - object: every key in expected must match the same key in actual (extra actual keys ignored)
 * - array: each expected item must find an unused match in actual; with `exactLists` sizes must equal;
 *   with `inOrder` matching index must equal expected index
 * - string starting with `^` and ending with `$` is treated as a regex against actual.toString()
 * - numbers compared as BigDecimal floored to scale 7
 */
object JsonSubsetMatcher {

  private val NUMERIC_REGEX = Regex("(\\d+\\.\\d+|\\d+)")

  fun isJsonSubset(expected: JsonNode, actual: JsonNode, options: SubsetOptions = SubsetOptions()): Boolean =
    verify(expected, actual, options)

  fun assertJsonSubset(
    expected: JsonNode,
    actual: JsonNode,
    options: SubsetOptions = SubsetOptions(),
    subject: String = "response",
  ) {
    val mismatch = findMismatch(expected, actual, options, "$") ?: return
    throw AssertionError(
      "Detail did not match $subject at $mismatch.\n\nExpected: $expected\n\nActual: $actual",
    )
  }

  fun assertNotJsonSubset(
    expected: JsonNode,
    actual: JsonNode,
    options: SubsetOptions = SubsetOptions(),
    subject: String = "response",
  ) {
    if (verify(expected, actual, options)) {
      throw AssertionError(
        "Detail matched $subject when it should not.\n\nExpected: $expected\n\nActual: $actual",
      )
    }
  }

  private fun verify(expected: JsonNode, actual: JsonNode, options: SubsetOptions): Boolean = when {
    expected.isNull && actual.isNull -> true
    expected.isNull || actual.isNull -> false
    isNumeric(expected) && isNumeric(actual) -> compareNumeric(expected, actual)
    expected is ObjectNode && actual is ObjectNode -> verifyObject(expected, actual, options)
    expected is ArrayNode && actual is ArrayNode -> verifyArray(expected, actual, options)
    isRegex(expected) -> Regex(expected.asText()).matches(actual.asText())
    else -> expected.asText().trim() == actual.asText().trim()
  }

  private fun verifyObject(expected: ObjectNode, actual: ObjectNode, options: SubsetOptions): Boolean =
    expected.fields().asSequence().all { (key, expectedValue) ->
      val actualValue = actual.get(key) ?: return@all false
      verify(expectedValue, actualValue, options)
    }

  private fun verifyArray(expected: ArrayNode, actual: ArrayNode, options: SubsetOptions): Boolean {
    val sizeOk = if (options.exactLists) expected.size() == actual.size() else expected.size() <= actual.size()
    if (!sizeOk) return false

    val consumed = mutableSetOf<Int>()
    expected.forEachIndexed { expectedIndex, expectedItem ->
      val matchedIndex = findMatch(actual, expectedItem, consumed, options)
      if (matchedIndex < 0) return false
      if (options.inOrder && matchedIndex != expectedIndex) return false
      consumed += matchedIndex
    }
    return true
  }

  private fun findMatch(
    actual: ArrayNode,
    expectedItem: JsonNode,
    consumed: Set<Int>,
    options: SubsetOptions,
  ): Int {
    for (i in 0 until actual.size()) {
      if (i in consumed) continue
      if (verify(expectedItem, actual[i], options)) return i
    }
    return -1
  }

  private fun findMismatch(
    expected: JsonNode,
    actual: JsonNode,
    options: SubsetOptions,
    path: String,
  ): String? = when {
    expected.isNull && actual.isNull -> null
    expected.isNull -> "$path: expected null but got $actual"
    actual.isNull -> "$path: expected $expected but got null"
    isNumeric(expected) && isNumeric(actual) ->
      if (compareNumeric(expected, actual)) null
      else "$path: expected ${expected.asText()} but got ${actual.asText()}"
    expected is ObjectNode && actual is ObjectNode -> findObjectMismatch(expected, actual, options, path)
    expected is ObjectNode -> "$path: expected object $expected but got ${actual.nodeType} $actual"
    expected is ArrayNode && actual is ArrayNode -> findArrayMismatch(expected, actual, options, path)
    expected is ArrayNode -> "$path: expected array but got ${actual.nodeType} $actual"
    isRegex(expected) ->
      if (Regex(expected.asText()).matches(actual.asText())) null
      else "$path: regex ${expected.asText()} did not match \"${actual.asText()}\""
    else ->
      if (expected.asText().trim() == actual.asText().trim()) null
      else "$path: expected \"${expected.asText()}\" but got \"${actual.asText()}\""
  }

  private fun findObjectMismatch(
    expected: ObjectNode,
    actual: ObjectNode,
    options: SubsetOptions,
    path: String,
  ): String? {
    for ((key, expectedValue) in expected.fields()) {
      val actualValue = actual.get(key) ?: return "$path.$key: missing in actual"
      val childMismatch = findMismatch(expectedValue, actualValue, options, "$path.$key")
      if (childMismatch != null) return childMismatch
    }
    return null
  }

  private fun findArrayMismatch(
    expected: ArrayNode,
    actual: ArrayNode,
    options: SubsetOptions,
    path: String,
  ): String? {
    if (options.exactLists && expected.size() != actual.size()) {
      return "$path: expected exactly ${expected.size()} items but got ${actual.size()}"
    }
    if (!options.exactLists && expected.size() > actual.size()) {
      return "$path: expected at least ${expected.size()} items but got ${actual.size()}"
    }

    if (options.inOrder) {
      for (i in 0 until expected.size()) {
        val childMismatch = findMismatch(expected[i], actual[i], options, "$path[$i]")
        if (childMismatch != null) return childMismatch
      }
      return null
    }

    val consumed = mutableSetOf<Int>()
    expected.forEachIndexed { expectedIndex, expectedItem ->
      val matchedIndex = findMatch(actual, expectedItem, consumed, options)
      if (matchedIndex < 0) return "$path[$expectedIndex]: no matching item found in actual"
      consumed += matchedIndex
    }
    return null
  }

  private fun isRegex(node: JsonNode): Boolean {
    if (!node.isTextual) return false
    val text = node.asText()
    return text.startsWith("^") && text.endsWith("$")
  }

  private fun isNumeric(node: JsonNode): Boolean =
    node.isNumber || (node.isTextual && NUMERIC_REGEX.matches(node.asText()))

  private fun compareNumeric(expected: JsonNode, actual: JsonNode): Boolean = try {
    val expectedScaled = BigDecimal(expected.asText()).setScale(7, RoundingMode.FLOOR)
    val actualScaled = BigDecimal(actual.asText()).setScale(7, RoundingMode.FLOOR)
    expectedScaled == actualScaled
  } catch (_: NumberFormatException) {
    false
  }
}
