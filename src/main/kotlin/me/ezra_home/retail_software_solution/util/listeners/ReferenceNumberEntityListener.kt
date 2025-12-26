package me.ezra_home.retail_software_solution.util.listeners

import jakarta.persistence.PrePersist
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component


@Component
class ReferenceNumberEntityListener {

    @Autowired
    fun setApplicationContext(context: ApplicationContext) {
        applicationContext = context
    }

    companion object {
        private val log = LoggerFactory.getLogger(ReferenceNumberEntityListener::class.java)
        private var applicationContext: ApplicationContext? = null
    }

    @PrePersist
    fun generateReferenceNumber(entity: Any) {
        try {
            val hasReferenceAnnotation = entity.javaClass.getAnnotation(HasReference::class.java)
                ?: return

            val referenceNumberField = try {
                entity.javaClass.getDeclaredField("referenceNumber")
            } catch (_: NoSuchFieldException) {
                log.warn("Entity ${entity.javaClass.simpleName} has @HasReference but no referenceNumber field")
                return
            }

            referenceNumberField.isAccessible = true
            val currentValue = referenceNumberField.get(entity) as? String
            if (!currentValue.isNullOrBlank()) {
                log.debug("Reference number already set for ${entity.javaClass.simpleName}: $currentValue")
                return
            }

            val generationService = applicationContext?.getBean<ReferenceNumberGenerator>()
            if (generationService == null) {
                log.error("ReferenceNumberGenerationService not available in Spring context")
                return
            }

            val tableName = hasReferenceAnnotation.tableName
            val referenceNumber = generationService.generateSingle(tableName)
            referenceNumberField.set(entity, referenceNumber)
            log.debug("Generated reference number for ${entity.javaClass.simpleName}: $referenceNumber")

        } catch (e: Exception) {
            log.error("Error generating reference number for entity ${entity.javaClass.simpleName}", e)
        }
    }
}
