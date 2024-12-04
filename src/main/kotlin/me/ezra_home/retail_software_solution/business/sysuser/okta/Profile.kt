package me.ezra_home.retail_software_solution.business.sysuser.okta

data class Profile(
    var firstName: String,
    var lastName: String,
    var mobilePhone: String?,
    var secondEmail: String?,
    var login: String,
    var email: String
)
