package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.enums.DataType
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.UNIT)
class UnitEntity(

    @Column(name = "name", nullable = false)
    var name: String? = null,

    @Column(name = "code", nullable = false)
    var code: String? = null,

    @Column(name = "data_type", nullable = false)
    var dataType: DataType? = null,

    @Column(name = "decimal_count")
    var decimalCount: Short? = null,

    @Column(name = "enumerated")
    var enumerated: Boolean? = null,

    @Column(name = "enumeration_options")
    var enumerationOptions: String? = null,

): AuditableEntity()