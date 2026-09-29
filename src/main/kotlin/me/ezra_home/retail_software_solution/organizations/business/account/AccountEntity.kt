package me.ezra_home.retail_software_solution.organizations.business.account

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.model.HasCreatorEntity
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import org.hibernate.envers.NotAudited
import java.math.BigDecimal
import java.time.Instant

@Audited
@Entity
@Table(name = TableNames.ACCOUNT)
class AccountEntity(

    @NotAudited
    @Column(name = "code", nullable = false, updatable = false, unique = true)
    var code: String,

    @Column(name = "name", length = 150, nullable = false)
    var name: String,

    @NotAudited
    @Column(name = "account_type", nullable = false, updatable = false)
    @Convert(converter = AccountTypeConverter::class)
    var accountType: AccountType,

    @NotAudited
    @Column(name = "currency_code", length = 3, nullable = false, updatable = false)
    var currencyCode: String,

    @Column(name = "is_active", nullable = false)
    var accountIsActive: Boolean = true,

    @NotAudited
    @Column(name = "is_system", nullable = false, updatable = false)
    var accountIsSystemMaintained: Boolean = false,

    @NotAudited
    @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
    var currentBalance: BigDecimal = BigDecimal.ZERO,

    @NotAudited
    @Column(name = "balance_updated_at")
    var balanceUpdatedAt: Instant? = null,

    @NotAudited
    @Column(name = "parent_account_code", updatable = false)
    var parentAccountCode: String? = null

) : HasCreatorEntity()
