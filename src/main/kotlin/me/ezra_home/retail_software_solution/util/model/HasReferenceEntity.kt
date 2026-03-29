package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import net.minidev.json.annotate.JsonIgnore

@MappedSuperclass
@EntityListeners(ReferenceNumberEntityListener::class)
abstract class HasReferenceEntity(
    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null

): HasCreatorEntity() {

    @JsonIgnore
    fun getNullSafeReferenceNumber(): String {
        return referenceNumber ?: throw IllegalStateException("Reference number is not set")
    }
}
