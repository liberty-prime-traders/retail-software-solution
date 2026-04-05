package me.ezra_home.retail_software_solution.util.async

internal interface AsyncExecutor {
  fun execute(block: () -> Unit)
}
