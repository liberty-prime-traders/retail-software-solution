package me.ezra_home.retail_software_solution.organizations.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import me.ezra_home.retail_software_solution.util.annotations.HasReference
import me.ezra_home.retail_software_solution.util.model.HasReferenceEntity
import me.ezra_home.retail_software_solution.util.model.TableName
import me.ezra_home.retail_software_solution.util.model.TableNames
import org.hibernate.envers.Audited

@Audited
@Entity
@Table(name = TableNames.ADDRESS)
@HasReference(tableName = TableName.ADDRESS)
internal class AddressEntity(

    @Column(name = "line1", length = 100)
    var line1: String? = null,

    @Column(name = "line2", length = 50)
    var line2: String? = null,

    @Column(name = "line3", length = 30)
    var line3: String? = null,

    @Column(name = "state", length = 100)
    var state: String? = null,

    @Column(name = "postal_code", length = 10)
    var postalCode: String? = null,

    @Column(name = "country", length = 100)
    var country: String? = null

): HasReferenceEntity()
