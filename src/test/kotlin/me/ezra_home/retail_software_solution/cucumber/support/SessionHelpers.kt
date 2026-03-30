package me.ezra_home.retail_software_solution.cucumber.support

import io.restassured.response.Response
import me.ezra_home.retail_software_solution.configuration.session.SessionContext
import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider

internal fun <T> withSession(session: SessionContext, block: () -> T): T {
  SessionContextProvider.setSession(session)
  return try {
    block()
  } finally {
    SessionContextProvider.clear()
  }
}

fun Response.getResponseId(): String =
  checkNotNull(jsonPath().getString("id")) { "Response missing 'id' field" }
