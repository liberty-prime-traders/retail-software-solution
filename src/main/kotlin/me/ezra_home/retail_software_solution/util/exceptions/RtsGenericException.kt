package me.ezra_home.retail_software_solution.util.exceptions

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

open class RtsGenericException(override val message: String, val payload: Any? = null)
    : ResponseStatusException(HttpStatus.BAD_REQUEST, message)
