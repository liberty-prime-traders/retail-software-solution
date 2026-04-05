package me.ezra_home.retail_software_solution.util.model

import jakarta.persistence.PrePersist
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.business.reference_number.ReferenceNumberGenerator
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component(ReferenceNumberEntityListener.BEAN_NAME)
class ReferenceNumberEntityListener {


    @Autowired
    fun setApplicationContext(context: ApplicationContext) {
        applicationContext = context
    }

    companion object {
        const val BEAN_NAME = "referenceNumberEntityListener"
        private val log = LoggerFactory.getLogger(ReferenceNumberEntityListener::class.java)
        private var applicationContext: ApplicationContext? = null
    }

    @PrePersist
    fun generateReferenceNumber(entity: HasReferenceEntity) {
        try {
            val currentValue = entity.referenceNumber
            if (!currentValue.isNullOrBlank()) return
            val hasReferenceAnnotation = entity.javaClass.getAnnotation(HasReference::class.java) ?: return
            val tableName = hasReferenceAnnotation.tableName
            val referenceNumberGenerator = applicationContext?.getBean<ReferenceNumberGenerator>()
            if (referenceNumberGenerator == null) {
                log.error("ReferenceNumberGenerator not available in Spring context")
                return
            }
            val referenceNumber = referenceNumberGenerator.generateSingle(tableName)
            entity.referenceNumber = referenceNumber
            log.debug("Generated reference number for ${entity.javaClass.simpleName}: $referenceNumber")
        } catch (e: Exception) {
            log.error("Error generating reference number for entity ${entity.javaClass.simpleName}", e)
            throw e
        }
    }

}
