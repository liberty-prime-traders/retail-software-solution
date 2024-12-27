package me.ezra_home.retail_software_solution.business.util.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

open class RtsGenericException(override val message: String)
    : ResponseStatusException(HttpStatus.BAD_REQUEST, message)
