package me.ezra_home.retail_software_solution.cucumber.hooks

import io.cucumber.java.Before
import io.cucumber.java.After
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import org.springframework.beans.factory.annotation.Autowired

class TestHooks {

  @Autowired
  private lateinit var testContext: TestContext

  @Autowired
  private lateinit var testDataManager: TestDataManager

  @Before
  fun beforeScenario() {
    testContext.reset()
  }

  @After
  fun afterScenario() {
    testDataManager.clear()
  }
}
