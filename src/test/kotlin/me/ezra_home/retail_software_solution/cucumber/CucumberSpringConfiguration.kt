package me.ezra_home.retail_software_solution.cucumber

import io.cucumber.spring.CucumberContextConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CucumberSpringConfiguration {

  @LocalServerPort
  var port: Int = 0

  companion object {
    private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
      .withDatabaseName("rtss_test")
      .withUsername("test_user")
      .withPassword("test_pass")

    private val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))

    init {
      postgres.start()
      kafka.start()
    }

    @JvmStatic
    @DynamicPropertySource
    fun configureProperties(registry: DynamicPropertyRegistry) {
      registry.add("spring.datasource.url", postgres::getJdbcUrl)
      registry.add("spring.datasource.username", postgres::getUsername)
      registry.add("spring.datasource.password", postgres::getPassword)
      registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
    }
  }
}
