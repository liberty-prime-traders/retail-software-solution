package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.enums.Status
import me.ezra_home.retail_software_solution.util.enums.StatusConverter
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.RESERVED_SUBDOMAIN)
@HasReference(tableName = TableName.RESERVED_SUBDOMAIN)
class ReservedSubdomainEntity(
    @Column(name = "subdomain", updatable = false)
     var subdomain: String? = null,

    @Column(name = "status")
    @Convert(converter = StatusConverter::class)
     var status: Status? = null

): HasReferenceEntity()
