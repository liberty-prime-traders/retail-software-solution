package me.ezra_home.retail_software_solution.cucumber.hooks

import io.cucumber.java.After
import io.cucumber.java.Before
import jakarta.annotation.PostConstruct
import me.ezra_home.retail_software_solution.configuration.cache.CacheSchemaLevel
import me.ezra_home.retail_software_solution.cucumber.support.context.AuthContext
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.ResponseContext
import me.ezra_home.retail_software_solution.cucumber.support.KafkaConsumerTestSupport
import me.ezra_home.retail_software_solution.cucumber.support.TestDatabaseCleaner
import me.ezra_home.retail_software_solution.cucumber.support.initialization.BoilerPlateDataInitializer
import me.ezra_home.retail_software_solution.util.enums.SchemaLevel
import org.springframework.beans.factory.getBeansWithAnnotation
import org.springframework.util.ClassUtils
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.CacheConfig
import org.springframework.context.ApplicationContext

class TestHooks(
  private val responseContext: ResponseContext,
  private val testDatabaseCleaner: TestDatabaseCleaner,
  private val kafkaConsumerTestSupport: KafkaConsumerTestSupport,
  private val injectContext: InjectContext,
  private val authContext: AuthContext,
  private val cacheManager: CacheManager,
  private val applicationContext: ApplicationContext,
  private val boilerPlateDataInitializer: BoilerPlateDataInitializer
) {

  private lateinit var transientCacheNames: Set<String>

  @PostConstruct
  fun resolveTransientCaches() {
    transientCacheNames = applicationContext.getBeansWithAnnotation<CacheSchemaLevel>().values
      .map { ClassUtils.getUserClass(it) }
      .filter { type ->
        val level = type.getAnnotation(CacheSchemaLevel::class.java)?.schemaLevel
        level == SchemaLevel.ORGANIZATION || level == SchemaLevel.LOCATION
      }
      .flatMap { type ->
        type.getAnnotation(CacheConfig::class.java)?.cacheNames?.toList() ?: emptyList()
      }
      .toSet()
  }

  @Before
  fun beforeScenario() {
    testDatabaseCleaner.clean()
    injectContext.clear()
    transientCacheNames.forEach { cacheManager.getCache(it)?.clear() }
    boilerPlateDataInitializer.restoreLocation()
    authContext.initialize()
    responseContext.reset()
  }

  @Before("@consumes-from-kafka")
  fun beforeKafkaConsumerScenario() {
    kafkaConsumerTestSupport.prepareConsumerScenario()
  }

  @After("@consumes-from-kafka")
  fun stopKafkaListenersForConsumerScenarios() {
    kafkaConsumerTestSupport.stopKafkaListeners()
  }
}
