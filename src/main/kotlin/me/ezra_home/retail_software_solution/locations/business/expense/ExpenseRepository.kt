package me.ezra_home.retail_software_solution.locations.business.expense

import me.ezra_home.retail_software_solution.cross_tier.expense.repository.ExpenseRepositoryBase
import org.springframework.stereotype.Repository

@Repository
interface ExpenseRepository : ExpenseRepositoryBase<ExpenseEntity>
