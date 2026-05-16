package me.ezra_home.retail_software_solution.organizations.business.adjustment_reason

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.organizations.business.adjustment_reason.api.AdjustmentDirection
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.ADJUSTMENT_REASON)
class AdjustmentReasonEntity(

    @Column(name = "name", nullable = false, length = 100)
    var name: String,

    @Column(name = "code", length = 5, updatable = false)
    var code: String? = null,

    @Convert(converter = AdjustmentDirectionConverter::class)
    @Column(name = "direction", nullable = false, length = 5)
    var direction: AdjustmentDirection,

    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

) : HasCreatorEntity()
