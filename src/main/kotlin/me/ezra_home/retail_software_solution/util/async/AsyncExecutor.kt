package me.ezra_home.retail_software_solution.util.async

interface AsyncExecutor {
  fun execute(block: () -> Unit)
}
