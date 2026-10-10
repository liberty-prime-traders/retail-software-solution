package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.util.ui_models.TreeNode

data class AccountsTreesForSelection(
    val payable: List<TreeNode<String>>,
    val recoverable: List<TreeNode<String>>,
    val paymentMethods: List<TreeNode<String>>,
    val expenseTypes: List<TreeNode<String>>,
)
