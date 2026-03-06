package me.ezra_home.retail_software_solution.cucumber.hooks

import io.cucumber.java.Before
import io.cucumber.java.After
import me.ezra_home.retail_software_solution.cucumber.config.KafkaConsumerTestSupport
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDatabaseCleaner
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.beans.factory.annotation.Autowired

class TestHooks {

  @Autowired
  private lateinit var testContext: TestContext

  @Autowired
  private lateinit var testDataManager: TestDataManager

  @Autowired
  private lateinit var testDatabaseCleaner: TestDatabaseCleaner

  @Autowired
  private lateinit var kafkaConsumerTestSupport: KafkaConsumerTestSupport

  @Before
  fun beforeScenario() {
    testDatabaseCleaner.clean()
    testContext.reset()
  }

  @After
  fun afterScenario() {
    @Suppress("UNCHECKED_CAST")
    (testContext.get("kafka.consumer", KafkaConsumer::class.java) as? KafkaConsumer<String, String>)?.close()
    testDataManager.clear()
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
