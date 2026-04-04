package me.ezra_home.retail_software_solution.cucumber.support.context

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Component

@Component
class KafkaContext {
  var lastCatalogEvent: JsonNode? = null

  fun reset() {
    lastCatalogEvent = null
  }
}
