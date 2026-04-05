package me.ezra_home.retail_software_solution.organizations.business.payment_method

import me.ezra_home.retail_software_solution.organizations.business.payment_method.PaymentMethodEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface PaymentMethodRepository: JpaRepository<PaymentMethodEntity, UUID>
