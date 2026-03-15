package me.ezra_home.retail_software_solution.cucumber.config

import io.cucumber.spring.CucumberContextConfiguration
import me.ezra_home.retail_software_solution.TestMockBeansConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMockBeansConfiguration::class, TestSecurityConfiguration::class)
class CucumberSpringConfiguration {

  @LocalServerPort
  var port: Int = 0
}
