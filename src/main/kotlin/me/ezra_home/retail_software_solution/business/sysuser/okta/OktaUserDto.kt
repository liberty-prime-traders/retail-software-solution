package me.ezra_home.retail_software_solution.business.sysuser.okta

import me.ezra_home.retail_software_solution.model.enums.Status

data class OktaUserDto(
    var id: String?,
    var status: Status?,
    var profile: Profile
)
