package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.PRODUCT_GROUP)
@HasReference(tableName = TableName.PRODUCT_GROUP)
class ProductGroupEntity(

    @Column(name = "group_name", nullable = false)
    var groupName: String,

    @Column(name = "description")
    var description: String? = null

): HasReferenceEntity()
