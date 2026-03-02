package me.ezra_home.retail_software_solution.cucumber

import io.cucumber.spring.CucumberContextConfiguration
import me.ezra_home.retail_software_solution.cucumber.config.TestSecurityConfiguration
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(CucumberSpringConfiguration.TestConfig::class, TestSecurityConfiguration::class)
class CucumberSpringConfiguration {

  @LocalServerPort
  var port: Int = 0

  @TestConfiguration
  class TestConfig {
    @Bean
    @Primary
    fun jwtDecoder(): JwtDecoder = mock()
    
    @Bean
    @Primary
    fun oktaClient(): com.okta.sdk.client.Client = mock()
  }
}
