package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.TITLE)
class TitleEntity (
    @Column(name = "value")
    var value: String? = null,
) : AuditableEntity()