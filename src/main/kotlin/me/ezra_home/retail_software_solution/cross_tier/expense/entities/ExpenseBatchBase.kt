package me.ezra_home.retail_software_solution.cross_tier.expense.entities

import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.MappedSuperclass
import me.ezra_home.retail_software_solution.util.model.ImmutableEntity

@MappedSuperclass
abstract class ExpenseBatchBase(

    @Column(name = "description", nullable = false, updatable = false)
    var description: String,

    @Convert(converter = ExpenseSourceTypeConverter::class)
    @Column(name = "source_type", nullable = false, updatable = false, length = 5)
    var sourceType: ExpenseSourceType,

    @Column(name = "source_reference", updatable = false)
    var sourceReference: String?

) : ImmutableEntity()
