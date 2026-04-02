package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.response.Response
import java.util.UUID

fun Response.getResponseId(): UUID {
  val id = checkNotNull(jsonPath().getString("id")) { "Response missing 'id' field" }
  return UUID.fromString(id)
}
