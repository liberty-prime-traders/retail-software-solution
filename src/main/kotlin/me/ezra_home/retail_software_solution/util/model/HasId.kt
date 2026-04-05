package me.ezra_home.retail_software_solution.util.model

import java.util.UUID

interface HasId {
    val id: UUID?

    fun getNullSafeId(): UUID = id ?: throw IllegalStateException("ID is not set")
}
