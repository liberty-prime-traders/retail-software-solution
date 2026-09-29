package me.ezra_home.retail_software_solution.organizations.business.account.api

import me.ezra_home.retail_software_solution.util.ui_models.TreeNode

data class AccountsTreesForSelection(
    val payable: List<TreeNode<String>>,
    val recoverable: List<TreeNode<String>>,
    val paymentMethods: List<TreeNode<String>>,
)
