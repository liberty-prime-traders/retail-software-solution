package me.ezra_home.retail_software_solution.util.exceptions

import org.springframework.http.HttpStatus

sealed class AuthException(payload: Any? = null)
    : RtsGenericException("Login Failed", payload, HttpStatus.UNAUTHORIZED) {

    data class AuthFailure(val code: AuthFailureCode, val data: Any? = null)

    enum class AuthFailureCode {
        INVALID_CREDENTIAL, ACCOUNT_DISABLED, PROVIDER_MISMATCH
    }

    class InvalidCredential : AuthException(AuthFailure(AuthFailureCode.INVALID_CREDENTIAL))

    class AccountDisabled : AuthException(AuthFailure(AuthFailureCode.ACCOUNT_DISABLED))

    class ProviderMismatch(pendingLinkToken: String)
        : AuthException(AuthFailure(AuthFailureCode.PROVIDER_MISMATCH, mapOf("pendingLinkToken" to pendingLinkToken)))
}
