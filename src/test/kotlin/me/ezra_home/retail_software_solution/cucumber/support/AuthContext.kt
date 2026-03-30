package me.ezra_home.retail_software_solution.cucumber.support

import java.util.UUID

object AuthContext {

  private data class Snapshot(val authToken: String?, val organizationId: UUID?, val locationId: UUID?)

  private val context = ThreadLocal.withInitial { Snapshot(null, null, null) }

  var authToken: String?
    get() = context.get().authToken
    set(value) { context.set(context.get().copy(authToken = value)) }

  var currentOrganizationId: UUID?
    get() = context.get().organizationId
    set(value) { context.set(context.get().copy(organizationId = value)) }

  var currentLocationId: UUID?
    get() = context.get().locationId
    set(value) { context.set(context.get().copy(locationId = value)) }

  fun reset() = context.remove()
}
