package me.ezra_home.retail_software_solution.cucumber.steps.platform

import io.cucumber.java.en.Given
import me.ezra_home.retail_software_solution.configuration.datasource.DataSourceBeanNames
import me.ezra_home.retail_software_solution.cucumber.fixtures.platform.AuthorizationPassFixtureBuilder
import me.ezra_home.retail_software_solution.cucumber.support.context.InjectContext
import me.ezra_home.retail_software_solution.cucumber.support.context.TransientKey
import me.ezra_home.retail_software_solution.support.TestConstants
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID
import javax.sql.DataSource

class AuthorizationPassSteps(
    private val fixtureBuilder: AuthorizationPassFixtureBuilder,
    private val injectContext: InjectContext,
    @Qualifier(DataSourceBeanNames.PLATFORM_SCHEMA_DATA_SOURCE)
    dataSource: DataSource
) {
    private val jdbcTemplate = JdbcTemplate(dataSource)

    @Given("an authorization pass exists")
    fun createPass() {
        injectContext.store(TransientKey.AUTHORIZATION_PASS, fixtureBuilder.create())
    }

    @Given("the authorization pass table is clean")
    fun cleanupPasses() {
        // These tables are protected in TestDatabaseCleaner, so we clean them manually here
        jdbcTemplate.execute("TRUNCATE TABLE platform.${TableNames.AUTHORIZATION_PASS} RESTART IDENTITY CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE platform.${TableNames.AUTHORIZATION_PASS_AUDIT} RESTART IDENTITY CASCADE")
    }
}
