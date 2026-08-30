package me.ezra_home.retail_software_solution.platform.business.jurisdiction_type

import me.ezra_home.retail_software_solution.AbstractIntegrationTest
import me.ezra_home.retail_software_solution.configuration.datasource.TransactionalOnPlatformSchema
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

// Insert paths map a just-saved entity straight onto a DTO whose createdOn is non-nullable, so
// every persist-time default must be readable without a flush. JurisdictionTypeEntity is just the
// vehicle: a concrete HasCreatorEntity subclass on the platform schema, needing no organization in
// session. It sits in this package because ArchitectureTest covers test classes, and reaching a
// domain's entity or repository from outside that domain is a violation.
class CreatedOnPrePersistTest : AbstractIntegrationTest() {

    @Autowired
    private lateinit var jurisdictionTypeRepository: JurisdictionTypeRepository

    @Test
    @TransactionalOnPlatformSchema
    fun `persist-time defaults are populated by save without an explicit flush`() {
        val saved = jurisdictionTypeRepository.save(JurisdictionTypeEntity(name = "prepersist-probe"))

        // All three come from separate persist hooks — HasCreatorEntity.prePersist,
        // BaseEntity.prePersist and ReferenceNumberEntityListener — and each must survive the
        // others being invoked on the same insert.
        assertNotNull(saved.createdOn, "createdOn should be stamped during persist, before any flush")
        assertNotNull(saved.id, "BaseEntity.prePersist should still generate the id")
        assertNotNull(saved.referenceNumber, "ReferenceNumberEntityListener should still run")
    }
}
