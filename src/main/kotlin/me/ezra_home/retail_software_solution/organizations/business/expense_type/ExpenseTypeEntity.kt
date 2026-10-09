package me.ezra_home.retail_software_solution.organizations.business.expense_type

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseSourceType
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited

@Audited
@Entity
@Table(name = TableNames.EXPENSE_TYPE)
class ExpenseTypeEntity(

    @NotAudited
    @Column(name = "code", updatable = false)
    var code: String? = null,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "expense_account_code", nullable = false)
    var expenseAccountCode: String,

    @Column(name = "eligible_payee_types", nullable = false)
    @Convert(converter = ExpensePayeeTypeSetConverter::class)
    var eligiblePayeeTypes: Set<ContactType>,

    @Column(name = "eligible_source_types", nullable = false)
    @Convert(converter = ExpenseSourceTypeSetConverter::class)
    var eligibleSourceTypes: Set<ExpenseSourceType>,

    @NotAudited
    @Column(name = "system_defined", nullable = false, updatable = false)
    var systemDefined: Boolean = false

) : HasCreatorEntity()
