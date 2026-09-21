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
import java.time.OffsetDateTime

@Entity
@Table(name = TableNames.SYS_USER)
@HasReference(tableName = TableName.SYS_USER)
class SysUserEntity(

    @Column(name = "email", length = 255)
    var email: String? = null,

    @Column(name = "disabled_at")
    var disabledAt: OffsetDateTime? = null,

    @Column(name = "local_first_name", length = 100, nullable = false)
    var localFirstName: String,

    @Column(name = "local_last_name", length = 100)
    var localLastName: String? = null,

    @Column(name = "user_type", nullable = false, length = 5)
    @Convert(converter = UserTypeConverter::class)
    var userType: UserType? = null

): HasReferenceEntity()
