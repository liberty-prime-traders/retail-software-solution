package me.ezra_home.retail_software_solution.organizations.business.contact

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Transient
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactIdentity
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactStatus
import me.ezra_home.retail_software_solution.organizations.business.contact.api.ContactType
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited
import java.math.BigDecimal

@Audited
@Entity
@Table(name = TableNames.CONTACT)
@HasReference(tableName = TableName.CONTACT)
class ContactEntity(

    @Convert(converter = ContactTypeConverter::class)
    @Column(name = "contact_type", nullable = false)
    var contactType: ContactType,

    @Column(name = "first_name", length = 255)
    var firstName: String? = null,

    @Column(name = "last_name", length = 255)
    var lastName: String? = null,

    @Column(name = "company_name", length = 255)
    var companyName: String? = null,

    @Column(name = "email", length = 255)
    var email: String? = null,

    @Column(name = "phone", length = 20)
    var phone: String? = null,

    @Column(name = "address")
    var address: String? = null,

    @Column(name = "credit_limit")
    var creditLimit: BigDecimal? = null,

    @Column(name = "notes")
    var notes: String? = null,

    @Convert(converter = ContactStatusConverter::class)
    @Column(name = "status", nullable = false)
    var status: ContactStatus = ContactStatus.ACTIVE

): HasReferenceEntity() {

    @get:Transient
    val organization: Boolean
        get() = !companyName.isNullOrBlank()

    @get:Transient
    val identity: ContactIdentity
        get() = try {
            if (organization) {
                ContactIdentity.Organization(companyName!!)
            } else {
                ContactIdentity.Individual(firstName.orEmpty(), lastName)
            }
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Invalid contact entity state: ${e.message}", e)
        }
}
