package me.ezra_home.retail_software_solution.util.queries

internal interface HasSearchStrategy<T : HasSearchStrategy<T>> {
    val searchStrategy: SearchStrategy

    fun copySelf(
        searchStrategy: SearchStrategy = this.searchStrategy
    ): T

    fun withSearchStrategy(newStrategy: SearchStrategy): T =
        copySelf(searchStrategy = newStrategy)
}

