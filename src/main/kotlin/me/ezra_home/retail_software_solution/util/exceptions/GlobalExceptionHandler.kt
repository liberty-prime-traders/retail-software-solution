package me.ezra_home.retail_software_solution.util.exceptions

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authorization.AuthorizationDeniedException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.Instant

@RestControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(RtsGenericException::class)
    fun handleRts(ex: RtsGenericException, req: HttpServletRequest): ResponseEntity<ApiError> {
        log.warn("Business error on ${req.method} ${req.requestURI}: ${ex.message}")
        return ResponseEntity
            .status(ex.statusCode)
            .body(ApiError(ex.message))
    }

    @ExceptionHandler(DataAccessException::class)
    fun handleDataAccess(ex: DataAccessException, req: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("DB error on ${req.method} ${req.requestURI}", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError("A database error occurred"))
    }

    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception, req: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Unhandled exception on ${req.method} ${req.requestURI}", ex)
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiError("An unexpected error occurred"))
    }

    @ExceptionHandler(AuthorizationDeniedException::class)
    fun handleAuthorizationDenied(): ResponseEntity<Unit> {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
    }

}

data class ApiError(
    val message: String,
    val timestamp: Instant = Instant.now()
)
