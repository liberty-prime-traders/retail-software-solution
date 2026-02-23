package me.ezra_home.retail_software_solution.cucumber.hooks

import io.cucumber.java.After
import io.cucumber.java.Before
import io.cucumber.java.Scenario
import jakarta.persistence.EntityManager
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

class TestHooks {

  @Autowired
  private lateinit var testContext: TestContext

  @Autowired
  private lateinit var testDataManager: TestDataManager

  @Autowired
  private lateinit var entityManager: EntityManager

  @Before
  fun beforeScenario() {
    testContext.reset()
  }

  @After
  @Transactional
  fun afterScenario() {
    cleanupDatabase()
    testDataManager.clear()
  }

  private fun cleanupDatabase() {
    entityManager.createNativeQuery("TRUNCATE TABLE products CASCADE").executeUpdate()
    entityManager.createNativeQuery("TRUNCATE TABLE organizations CASCADE").executeUpdate()
    entityManager.createNativeQuery("TRUNCATE TABLE locations CASCADE").executeUpdate()
  }
}
