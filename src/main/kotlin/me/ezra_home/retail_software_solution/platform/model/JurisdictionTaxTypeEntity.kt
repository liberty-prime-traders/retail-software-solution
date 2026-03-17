package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.JURISDICTION_TAX_TYPE)
class JurisdictionTaxTypeEntity(

    @Column(name = "tax_type_id", nullable = false, updatable = false)
    var taxTypeId: UUID,

    @Column(name = "jurisdiction_id", nullable = false, updatable = false)
    var jurisdictionId: UUID,

    @Column(name = "active", nullable = false)
    var active: Boolean = true

) : HasCreatorEntity()
