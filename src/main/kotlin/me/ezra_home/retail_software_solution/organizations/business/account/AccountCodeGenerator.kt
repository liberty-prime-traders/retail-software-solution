package me.ezra_home.retail_software_solution.organizations.business.account


object AccountCodeGenerator {

    private const val ORG_ROOT_START = 100

    fun generateChildCode(parentCode: String, siblings: List<AccountDto>): String {
        return withNumberFormatHandled {
            if (siblings.isEmpty()) {
                "$parentCode.001"
            } else {
                val maxSegment = siblings.maxOf { it.code.substringAfterLast(".").toInt() }
                val nextCodeSegment = maxSegment + 1
                if (nextCodeSegment > 999) {
                    throw IllegalStateException("Cannot generate more than 999 child accounts under the one parent.")
                }
                "$parentCode.${nextCodeSegment.toString().padStart(3, '0')}"
            }
        }
    }

    fun generateRootCode(accounts: List<AccountDto>): String {
        return withNumberFormatHandled {
            val orgRoots = accounts
                .filter { it.parentAccountCode == null }
                .mapNotNull { it.code.toIntOrNull() }
                .filter { it >= ORG_ROOT_START }
            val next = if (orgRoots.isEmpty()) ORG_ROOT_START else orgRoots.max() + 1
            next.toString().padStart(3, '0')
        }
    }

    private fun withNumberFormatHandled(block: () -> String): String {
        return try {
            block()
        } catch (_: NumberFormatException) {
            throw IllegalStateException("An existing account with a malformed code format was found")
        }
    }
}
