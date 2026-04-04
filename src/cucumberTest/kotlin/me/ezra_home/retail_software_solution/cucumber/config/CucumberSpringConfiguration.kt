package me.ezra_home.retail_software_solution.cucumber.config

import io.cucumber.spring.CucumberContextConfiguration
import me.ezra_home.retail_software_solution.TestMockBeansConfiguration
import me.ezra_home.retail_software_solution.cucumber.support.initialization.ContainerInitializer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.ContextConfiguration


@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test", "cucumber")
@Import(TestMockBeansConfiguration::class, TestSecurityConfiguration::class)
@ContextConfiguration(initializers = [ContainerInitializer::class])
class CucumberSpringConfiguration
