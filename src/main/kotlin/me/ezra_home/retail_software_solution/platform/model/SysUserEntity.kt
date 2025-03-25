package me.ezra_home.retail_software_solution.platform.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import me.ezra_home.retail_software_solution.util.model.BaseEntity
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.SYS_USER)
class SysUserEntity(
    @NotNull
    @Column(name = "okta_id", nullable = false, length = 50)
    var oktaId: String? = null

): BaseEntity()
