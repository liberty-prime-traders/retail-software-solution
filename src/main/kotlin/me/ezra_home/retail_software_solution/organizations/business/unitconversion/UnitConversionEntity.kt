package me.ezra_home.retail_software_solution.organizations.business.unitconversion

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.UNIT_CONVERSION)
class UnitConversionEntity(

    @Column(name = "from_unit_id", nullable = false)
    var fromUnitId: UUID,

    @Column(name = "to_unit_id", nullable = false)
    var toUnitId: UUID,

    @Column(name = "factor_numerator", nullable = false)
    var factorNumerator: Long,

    @Column(name = "factor_denominator", nullable = false)
    var factorDenominator: Long

) : HasCreatorEntity()
