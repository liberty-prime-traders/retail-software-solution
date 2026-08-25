package me.ezra_home.retail_software_solution.configuration.session

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
  val targetLocation = getBySchema(schemaName)
  val switched = original.copy(
    location = LocationSession(id = targetLocation.id, schemaName = schemaName, timezone = targetLocation.timezone)
  )
  SessionContextProvider.setSession(switched)
  return try {
    block()
  } finally {
    SessionContextProvider.setSession(original)
  }
}
