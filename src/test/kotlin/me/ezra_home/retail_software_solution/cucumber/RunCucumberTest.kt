package me.ezra_home.retail_software_solution.cucumber

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectClasspathResource
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(
  key = Constants.GLUE_PROPERTY_NAME,
  value = "me.ezra_home.retail_software_solution.cucumber"
)
@ConfigurationParameter(
  key = Constants.PLUGIN_PROPERTY_NAME,
  value = "pretty, html:build/reports/cucumber.html, json:build/reports/cucumber.json"
)
class RunCucumberTest
