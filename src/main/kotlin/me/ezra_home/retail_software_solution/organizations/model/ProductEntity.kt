package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.enums.ProductStatus
import me.ezra_home.retail_software_solution.util.enums.ProductStatusConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited

@Audited
@Entity
@Table(name = TableNames.PRODUCT)
@HasReference(tableName = TableName.PRODUCT)
class ProductEntity(

    @Column(name = "name", nullable = false)
    var productName: String,

    @Column(name = "description")
    var description: String? = null,

    @Column(name = "product_group_id", nullable = false)
    var productGroupId: UUID,

    @NotAudited
    @Column(name = "product_group_name", insertable = false, updatable = false)
    var productGroupName: String? = null,

    @Column(name = "base_unit_id")
    var baseUnitId: UUID,

    @Convert(converter = ProductStatusConverter::class)
    @Column(name = "status", nullable = false)
    var status: ProductStatus? = ProductStatus.ACTIVE

): HasReferenceEntity()
