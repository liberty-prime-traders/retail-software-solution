package me.ezra_home.retail_software_solution.platform.business.sysuser

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.platform.business.sysuser.api.UserType
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames

@Entity
@Table(name = TableNames.SYS_USER)
@HasReference(tableName = TableName.SYS_USER)
class SysUserEntity(

    @Column(name = "okta_id", nullable = false, length = 50)
    var oktaId: String? = null,

    @Column(name = "local_first_name", length = 100)
    var localFirstName: String? = null,

    @Column(name = "local_last_name", length = 100)
    var localLastName: String? = null,

    @Column(name = "user_type", nullable = false, length = 5)
    @Convert(converter = UserTypeConverter::class)
    var userType: UserType? = null

): HasReferenceEntity()
