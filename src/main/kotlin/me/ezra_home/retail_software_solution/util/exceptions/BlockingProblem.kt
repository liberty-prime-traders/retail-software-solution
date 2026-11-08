package me.ezra_home.retail_software_solution.util.exceptions

data class BlockingProblem(
    val documentReference: String,
    val description: String,
    val effect: String,
)
