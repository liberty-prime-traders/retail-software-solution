package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.ORGANIZATION)
class OrganizationEntity(

    @Column(name = "name")
    open var name: String,

    @Column(name = "description")
    open var description: String? = null

): AuditableEntity()
