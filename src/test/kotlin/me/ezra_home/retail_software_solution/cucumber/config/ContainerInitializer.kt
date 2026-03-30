package me.ezra_home.retail_software_solution.cucumber.config

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.test.context.support.TestPropertySourceUtils
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

class ContainerInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {

  companion object {
    private val postgres = PostgreSQLContainer<Nothing>("postgres:16").apply {
      withDatabaseName("rtss_e2e_test")
      withUsername("rtss_test_user")
      withPassword("rtss_test_password")
      start()
    }

    private val kafka = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0")).apply {
      start()
    }
  }

  override fun initialize(context: ConfigurableApplicationContext) {
    val jdbcUrl = "jdbc:postgresql://${postgres.host}:${postgres.getMappedPort(5432)}/${postgres.databaseName}"
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
      context,
      "spring.datasource.location.jdbc-url=$jdbcUrl",
      "spring.datasource.location.username=${postgres.username}",
      "spring.datasource.location.password=${postgres.password}",
      "spring.datasource.platform.jdbc-url=$jdbcUrl?currentSchema=platform",
      "spring.datasource.platform.username=${postgres.username}",
      "spring.datasource.platform.password=${postgres.password}",
      "spring.datasource.organization.jdbc-url=$jdbcUrl",
      "spring.datasource.organization.username=${postgres.username}",
      "spring.datasource.organization.password=${postgres.password}",
      "spring.kafka.bootstrap-servers=${kafka.bootstrapServers}"
    )
  }
}
