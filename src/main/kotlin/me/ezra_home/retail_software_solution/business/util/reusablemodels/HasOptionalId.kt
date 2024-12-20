package me.ezra_home.retail_software_solution.business.util.reusablemodels

import java.util.Optional
import java.util.UUID

interface HasOptionalId {
    val id: Optional<UUID>?

    fun getIdFromOptional(): UUID? {
        if (id == null) return null
        return id?.get()
    }
}
