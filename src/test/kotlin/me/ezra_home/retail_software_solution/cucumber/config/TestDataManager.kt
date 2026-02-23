package me.ezra_home.retail_software_solution.cucumber.config

import org.springframework.stereotype.Component
import java.util.UUID

@Component
class TestDataManager {

  private val entities = mutableMapOf<String, MutableList<UUID>>()

  fun track(type: String, id: UUID) {
    entities.getOrPut(type) { mutableListOf() }.add(id)
  }

  fun getAll(type: String): List<UUID> = entities[type] ?: emptyList()

  fun clear() = entities.clear()
}
