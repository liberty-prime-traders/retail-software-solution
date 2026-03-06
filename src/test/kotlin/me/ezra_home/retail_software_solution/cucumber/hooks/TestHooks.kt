package me.ezra_home.retail_software_solution.cucumber.hooks

import io.cucumber.java.Before
import io.cucumber.java.After
import me.ezra_home.retail_software_solution.cucumber.config.TestContext
import me.ezra_home.retail_software_solution.cucumber.config.TestDatabaseCleaner
import me.ezra_home.retail_software_solution.cucumber.config.TestDataManager
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.config.KafkaListenerEndpointRegistry

class TestHooks {

  @Autowired
  private lateinit var testContext: TestContext

  @Autowired
  private lateinit var testDataManager: TestDataManager

  @Autowired
  private lateinit var testDatabaseCleaner: TestDatabaseCleaner

  @Autowired
  private lateinit var kafkaListenerRegistry: KafkaListenerEndpointRegistry

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

  @After("@kafka-consumer")
  fun stopKafkaListenersForConsumerScenarios() {
    kafkaListenerRegistry.listenerContainers.forEach { container ->
      if (container.isRunning) {
        container.stop()
      }
    }
  }
}
