package me.ezra_home.retail_software_solution.platform.business.sysuser

import me.ezra_home.retail_software_solution.platform.business.sysuser.api.UserType
import me.ezra_home.retail_software_solution.util.model.HasId
import java.time.OffsetDateTime
import java.util.UUID

data class SysUserDto(
    override var id: UUID? = null,
    var createdById: UUID? = null,
    var createdOn: OffsetDateTime? = null,
    var referenceNumber: String? = null,
    var oktaId: String? = null,
    var localFirstName: String? = null,
    var localLastName: String? = null,
    var userType: UserType? = null
) : HasId
