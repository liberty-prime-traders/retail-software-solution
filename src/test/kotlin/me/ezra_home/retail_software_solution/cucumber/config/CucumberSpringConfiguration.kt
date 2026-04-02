package me.ezra_home.retail_software_solution.cucumber.config

import io.cucumber.spring.CucumberContextConfiguration
import me.ezra_home.retail_software_solution.TestMockBeansConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestMockBeansConfiguration::class, TestSecurityConfiguration::class)
@ContextConfiguration(initializers = [ContainerInitializer::class])
class CucumberSpringConfiguration {
}
