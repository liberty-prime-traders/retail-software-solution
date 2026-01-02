package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.ExpirableAssignmentEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import java.util.UUID

@Entity
@Table(name = TableNames.PRODUCT_TAG)
@HasReference(tableName = TableName.PRODUCT_TAG)
class ProductTagEntity(

    @Column(name = "product_id", nullable = false, updatable = false)
    var productId: UUID,

    @Column(name = "tag_id", nullable = false, updatable = false)
    var tagId: UUID

): ExpirableAssignmentEntity()
