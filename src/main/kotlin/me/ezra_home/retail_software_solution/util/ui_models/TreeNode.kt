package me.ezra_home.retail_software_solution.util.ui_models


data class TreeNode<KEY> (
    val key: KEY,
    val label: String,
    val selectable: Boolean = true,
    val children: List<TreeNode<KEY>> = emptyList()
)
