package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.platform.business.tax_type.CalculationMethod
import me.ezra_home.retail_software_solution.platform.business.tax_type.CalculationMethodConverter
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxApplicationLevel
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxApplicationLevelConverter
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxRecoveryType
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxRecoveryTypeConverter
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTrigger
import me.ezra_home.retail_software_solution.platform.business.tax_type.TaxTriggerConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.TAX_TYPE)
@HasReference(tableName = TableName.TAX_TYPE)
internal class TaxTypeEntity(

    @Column(name = "name", length = 100, nullable = false)
    var name: String,

    @Column(name = "description", length = 500)
    var description: String? = null,

    @Convert(converter = CalculationMethodConverter::class)
    @Column(name = "calculation_method", length = 5, nullable = false)
    var calculationMethod: CalculationMethod,

    @Convert(converter = TaxRecoveryTypeConverter::class)
    @Column(name = "tax_recovery_type", length = 5, nullable = false)
    var taxRecoveryType: TaxRecoveryType,

    @Convert(converter = TaxApplicationLevelConverter::class)
    @Column(name = "tax_application_level", length = 5, nullable = false)
    var taxApplicationLevel: TaxApplicationLevel,

    @Convert(converter = TaxTriggerConverter::class)
    @Column(name = "tax_triggers", nullable = false)
    var taxTriggers: List<TaxTrigger>

) : HasReferenceEntity()
