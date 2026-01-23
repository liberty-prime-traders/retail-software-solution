package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

interface FilterStrategy {
    fun apply(context: QueryBuilderContext)
}
