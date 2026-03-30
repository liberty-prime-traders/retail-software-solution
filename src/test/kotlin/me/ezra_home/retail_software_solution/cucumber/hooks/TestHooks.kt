package me.ezra_home.retail_software_solution.cucumber.hooks

import io.cucumber.java.Before
import io.cucumber.java.After
import me.ezra_home.retail_software_solution.cucumber.support.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.KafkaConsumerTestSupport
import me.ezra_home.retail_software_solution.cucumber.support.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.TestDatabaseCleaner

class TestHooks(
  private val responseContext: ResponseContext,
  private val testDatabaseCleaner: TestDatabaseCleaner,
  private val kafkaConsumerTestSupport: KafkaConsumerTestSupport,
  private val injectContext: InjectContext
) {

  @Before
  fun beforeScenario() {
    testDatabaseCleaner.clean()
    AuthContext.reset()
    responseContext.reset()
    injectContext.clear()
  }

  @Before("@kafka-consumer")
  fun beforeKafkaConsumerScenario() {
    kafkaConsumerTestSupport.prepareConsumerScenario()
  }

  @After("@kafka-consumer")
  fun stopKafkaListenersForConsumerScenarios() {
    kafkaConsumerTestSupport.stopKafkaListeners()
  }
}
