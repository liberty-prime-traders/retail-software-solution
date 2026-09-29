package me.ezra_home.retail_software_solution.util.ui_models


data class TreeNodeWithData<KEY, VALUE> (
    val key: KEY,
    val label: String,
    val selectable: Boolean = true,
    val data: VALUE? = null,
    val children: List<TreeNodeWithData<KEY, VALUE>> = emptyList()
)

typealias TreeNode<KEY> = TreeNodeWithData<KEY, Any?>
