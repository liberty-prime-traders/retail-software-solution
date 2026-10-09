package me.ezra_home.retail_software_solution.cross_tier.expense.kafka_handler

import me.ezra_home.retail_software_solution.configuration.session.SessionContextProvider
import me.ezra_home.retail_software_solution.cross_tier.expense.operation.ExpenseReissuer
import org.springframework.stereotype.Component

@Component
class ExpenseReissuerResolver(
    private val expenseReissuers: List<ExpenseReissuer>
) {

    fun current(): ExpenseReissuer {
        val isLocationLevel = SessionContextProvider.getLocationIdOrNull() != null
        return expenseReissuers.single { it.isLocationLevel == isLocationLevel }
    }
}
