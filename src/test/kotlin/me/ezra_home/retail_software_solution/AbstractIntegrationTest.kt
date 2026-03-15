package me.ezra_home.retail_software_solution

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Import(TestMockBeansConfiguration::class)
abstract class AbstractIntegrationTest
