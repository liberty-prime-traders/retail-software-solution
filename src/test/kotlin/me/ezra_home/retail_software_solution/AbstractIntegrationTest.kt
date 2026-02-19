package me.ezra_home.retail_software_solution

import com.okta.sdk.client.Client
import org.mockito.Mockito.mock
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(AbstractIntegrationTest.TestConfig::class)
abstract class AbstractIntegrationTest {

  @TestConfiguration
  class TestConfig {
    @Bean
    @Primary
    fun oktaClient(): Client = mock()

    @Bean
    @Primary
    fun jwtDecoder(): JwtDecoder = mock()
  }
}
