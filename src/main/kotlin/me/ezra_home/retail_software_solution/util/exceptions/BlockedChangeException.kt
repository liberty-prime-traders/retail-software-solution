package me.ezra_home.retail_software_solution.util.exceptions

class BlockedChangeException(
    headline: String,
    val problems: List<BlockingProblem>
) : RtsGenericException(headline, payload = problems) {

    companion object {

        fun throwIfAny(problems: List<BlockingProblem>) {
            if (problems.isEmpty()) return
            throw BlockedChangeException(headlineOf(problems), problems)
        }

        fun headlineOf(problems: List<BlockingProblem>): String =
            "This change cannot be saved. " +
                if (problems.size == 1) "1 thing needs fixing." else "${problems.size} things need fixing."

        fun contactSupport(problems: List<BlockingProblem>): BlockedChangeException = BlockedChangeException(
            "The stock records of this delivery do not add up, so it cannot be changed. Please contact support.",
            problems
        )
    }
}
