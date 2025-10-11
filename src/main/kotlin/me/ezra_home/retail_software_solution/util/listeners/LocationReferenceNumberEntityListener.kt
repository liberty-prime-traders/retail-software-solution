package me.ezra_home.retail_software_solution.util.listeners

import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.service.LocationReferenceNumberGeneratorService
import java.lang.reflect.Field

class LocationReferenceNumberEntityListener {
    companion object {
        lateinit var locationReferenceNumberGeneratorService: LocationReferenceNumberGeneratorService
    }

    @PrePersist
    fun setReferenceNumber(entity: Any) {
        val clazz = entity::class.java
        val referenceNumberField: Field? = clazz.declaredFields.find { it.name == "referenceNumber" }
        if (referenceNumberField != null) {
            referenceNumberField.isAccessible = true
            val currentValue = referenceNumberField.get(entity) as? String
            if (currentValue.isNullOrBlank()) {
                val tableAnnotation = clazz.getAnnotation(Table::class.java)
                val tableName = tableAnnotation?.name ?: clazz.simpleName
                val generatedReferenceNumber = locationReferenceNumberGeneratorService.generateReferenceNumber(tableName)
                referenceNumberField.set(entity, generatedReferenceNumber)
            }
        }
    }
}

