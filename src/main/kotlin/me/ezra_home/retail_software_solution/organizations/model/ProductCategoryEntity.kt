package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.PRODUCT_CATEGORY)
@HasReference(tableName = TableName.PRODUCT_CATEGORY)
internal class ProductCategoryEntity(

    @Column(name = "category_name", nullable = false)
    var categoryName: String? = null,

    @Column(name = "description")
    var description: String? = null
): HasReferenceEntity()
