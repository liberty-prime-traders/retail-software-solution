package me.ezra_home.retail_software_solution.organizations.business.product.search.filter_strategies

data class QueryBuilderContext(
    val whereClauses: MutableList<String> = mutableListOf(),
    val havingClauses: MutableList<String> = mutableListOf(),
    val params: MutableMap<String, Any> = mutableMapOf()
)
