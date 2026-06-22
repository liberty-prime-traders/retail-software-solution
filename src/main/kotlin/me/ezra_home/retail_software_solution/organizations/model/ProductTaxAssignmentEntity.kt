package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ExpirableDateAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.time.LocalDate
import java.util.UUID

@Audited
@Entity
@Table(name = TableNames.PRODUCT_TAX_ASSIGNMENT)
@HasReference(tableName = TableName.PRODUCT_TAX_ASSIGNMENT)
class ProductTaxAssignmentEntity(

    @Column(name = "product_id", nullable = false, updatable = false)
    var productId: UUID,

    @Column(name = "tax_rate_id", nullable = false, updatable = false)
    var taxRateId: UUID,

    startDate: LocalDate,
    endDate: LocalDate? = null

) : ExpirableDateAssignmentEntity(startDate, endDate)
