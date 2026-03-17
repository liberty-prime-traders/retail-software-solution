package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.enums.CalculationMethod
import me.ezra_home.retail_software_solution.util.enums.CalculationMethodConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.TAX_TYPE)
@HasReference(tableName = TableName.TAX_TYPE)
class TaxTypeEntity(

    @Column(name = "name", length = 100, nullable = false)
    var name: String,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Convert(converter = CalculationMethodConverter::class)
    @Column(name = "calculation_method", length = 5, nullable = false)
    var calculationMethod: CalculationMethod

) : HasReferenceEntity()
