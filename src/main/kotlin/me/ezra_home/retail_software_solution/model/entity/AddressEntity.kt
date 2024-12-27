package me.ezra_home.retail_software_solution.model.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.validation.constraints.Size
import me.ezra_home.retail_software_solution.model.util.TableNames

@Entity
@Table(name = TableNames.ADDRESS)
class AddressEntity(

    @Size(max = 100)
    @Column(name = "line1", length = 100)
    open var line1: String? = null,

    @Size(max = 50)
    @Column(name = "line2", length = 50)
    open var line2: String? = null,

    @Size(max = 30)
    @Column(name = "line3", length = 30)
    open var line3: String? = null,

    @Size(max = 100)
    @Column(name = "state", length = 100)
    open var state: String? = null,

    @Size(max = 10)
    @Column(name = "postal_code", length = 10)
    open var postalCode: String? = null,

    @Size(max = 100)
    @Column(name = "country", length = 100)
    open var country: String? = null

): AuditableEntity()
