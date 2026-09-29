package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.Column
import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.exceptions.RtsGenericException

@MappedSuperclass
@EntityListeners(ReferenceNumberEntityListener::class)
abstract class HasReferenceEntity(
    @Column(name = "reference_number", unique = true)
    var referenceNumber: String? = null

): HasCreatorEntity() {

    fun requiredReference(): String = referenceNumber
        ?: throw RtsGenericException("${javaClass.simpleName} is missing its reference number")
}
