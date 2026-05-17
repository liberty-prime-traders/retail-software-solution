package me.ezra_home.retail_software_solution.locations.business.sale_session.api

import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException
import java.util.UUID

data class SessionIdentity(
    val id: UUID? = null,
    val transientId: UUID? = null,
) {

    init {
        if ((id == null) == (transientId == null)) {
            throw RtsGenericException("SessionIdentity requires exactly one of id or transientId")
        }
    }

    fun key(): UUID = id ?: transientId!!

    fun isPersisted(): Boolean = id != null

    companion object {
        fun fresh(): SessionIdentity = SessionIdentity(transientId = UUID.randomUUID())
        fun persisted(id: UUID): SessionIdentity = SessionIdentity(id = id)
    }
}
