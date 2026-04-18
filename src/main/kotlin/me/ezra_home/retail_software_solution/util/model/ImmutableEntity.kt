package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PreUpdate
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

@MappedSuperclass
abstract class ImmutableEntity: HasReferenceEntity() {
    @PreUpdate
    fun onUpdate() {
        throw RtsGenericException("Cannot update an immutable entity ${this::class.simpleName}")
    }
}
