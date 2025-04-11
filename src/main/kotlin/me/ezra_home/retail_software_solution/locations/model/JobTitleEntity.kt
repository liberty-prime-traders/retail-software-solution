package me.ezra_home.retail_software_solution.locations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.AuditableEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.JOB_TITLE)
class JobTitleEntity (
    @Column(name = "value")
    var value: String? = null,
) : AuditableEntity()
