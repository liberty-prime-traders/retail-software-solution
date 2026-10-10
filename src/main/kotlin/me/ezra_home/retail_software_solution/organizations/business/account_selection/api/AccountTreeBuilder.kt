package me.ezra_home.retail_software_solution.organizations.business.account_selection.api

import me.ezra_home.retail_software_solution.organizations.business.account.api.AccountSelectionCandidate
import me.ezra_home.retail_software_solution.util.ui_models.TreeNode
import org.springframework.stereotype.Component

@Component
class AccountTreeBuilder {

    fun build(candidates: List<AccountSelectionCandidate>, rule: AccountSelectionRule): List<TreeNode<String>> {
        val activeChildrenByParentCode = candidates.filter { it.accountIsActive }.groupBy { it.parentAccountCode }
        return activeChildrenByParentCode[null].orEmpty()
            .mapNotNull { buildNode(it, activeChildrenByParentCode, rule) }
    }

    private fun buildNode(
        candidate: AccountSelectionCandidate,
        activeChildrenByParentCode: Map<String?, List<AccountSelectionCandidate>>,
        rule: AccountSelectionRule
    ): TreeNode<String>? {
        val children = activeChildrenByParentCode[candidate.code].orEmpty()
            .mapNotNull { buildNode(it, activeChildrenByParentCode, rule) }

        val selectable = rule.isSelectable(candidate)
        if (!selectable && children.isEmpty()) return null

        return TreeNode(
            key = candidate.code,
            label = candidate.label,
            selectable = selectable,
            children = children
        )
    }
}
