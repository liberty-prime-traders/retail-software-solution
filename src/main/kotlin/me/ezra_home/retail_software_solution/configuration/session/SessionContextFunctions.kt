package me.ezra_home.retail_software_solution.configuration.session

import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationDto
import me.ezra_home.retail_software_solution.organizations.business.location.api.LocationService

fun <T> withSession(session: SessionContext, block: () -> T): T {
  SessionContextProvider.setSession(session)
  return try {
    block()
  } finally {
    SessionContextProvider.clear()
  }
}

fun <T> LocationService.withLocationSchema(schemaName: String, block: () -> T): T {
  val original = SessionContextProvider.getSession()
  if (original.location == null) {
    throw IllegalStateException("withLocationSchema called with no location in session — set a location context before switching schemas")
  }
  return withLocationSession(getBySchema(schemaName)) { block() }
}

fun <T> withLocationSession(location: LocationDto?, block: () -> T): T {
  val original = SessionContextProvider.getSession()
  val switched = original.copy(
    location = location?.let { LocationSession(id = it.id, schemaName = it.schemaName!!, timezone = it.timezone) }
  )
  SessionContextProvider.setSession(switched)
  return try {
    block()
  } finally {
    SessionContextProvider.setSession(original)
  }
}
