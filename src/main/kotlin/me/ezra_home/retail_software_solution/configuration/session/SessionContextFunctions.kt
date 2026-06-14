package me.ezra_home.retail_software_solution.configuration.session

fun <T> withSession(session: SessionContext, block: () -> T): T {
  SessionContextProvider.setSession(session)
  return try {
    block()
  } finally {
    SessionContextProvider.clear()
  }
}

fun <T> withLocationSchema(schemaName: String, block: () -> T): T {
  val original = SessionContextProvider.getSession()
  val location = original.location
    ?: throw IllegalStateException("withLocationSchema called with no location in session — set a location context before switching schemas")
  val switched = original.copy(location = location.copy(schemaName = schemaName))
  SessionContextProvider.setSession(switched)
  return try {
    block()
  } finally {
    SessionContextProvider.setSession(original)
  }
}
