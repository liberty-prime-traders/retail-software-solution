package me.ezra_home.retail_software_solution.exception

class ResourceNotFoundException(message: String) : RuntimeException(message) {
    constructor(message: String, cause: Throwable) : this(message) {
        cause?.let { initCause(it) }
    }
}