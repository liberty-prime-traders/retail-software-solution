package me.ezra_home.retail_software_solution.cross_tier.expense.operation

import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpensePaymentVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseDto
import me.ezra_home.retail_software_solution.cross_tier.expense.model.ExpenseVoidDto
import me.ezra_home.retail_software_solution.cross_tier.expense.ExpenseTier
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpensePaymentVoidedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseRecordedEvent
import me.ezra_home.retail_software_solution.messaging.kafka.transaction.events.ExpenseVoidedEvent
import me.ezra_home.retail_software_solution.util.business.DateTimes
import org.springframework.context.ApplicationEventPublisher
import java.time.Instant
import java.util.UUID

class ExpenseEvents(
    private val expenseTier: ExpenseTier,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    fun publishRecorded(expenseDto: ExpenseDto) {
        applicationEventPublisher.publishEvent(
            ExpenseRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseTier.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                expenseId = expenseDto.id,
                expenseReferenceNumber = expenseDto.referenceNumber,
                expenseAccountCode = expenseDto.expenseAccountCode,
                payeeContactId = expenseDto.payeeContactId,
                amount = expenseDto.amount,
                expenseDate = expenseDto.expenseDate
            )
        )
    }

    fun publishVoided(expenseVoidDto: ExpenseVoidDto, expenseDto: ExpenseDto) {
        applicationEventPublisher.publishEvent(
            ExpenseVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseTier.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                voidId = expenseVoidDto.id,
                expenseId = expenseDto.id,
                expenseReferenceNumber = expenseDto.referenceNumber,
                expenseAccountCode = expenseDto.expenseAccountCode,
                payeeContactId = expenseDto.payeeContactId,
                amount = expenseDto.amount,
                voidedOn = DateTimes.Local.atOrganizationZone(expenseVoidDto.voidedOn)
            )
        )
    }

    fun publishPaymentRecorded(expensePaymentDto: ExpensePaymentDto, expenseDto: ExpenseDto) {
        applicationEventPublisher.publishEvent(
            ExpensePaymentRecordedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseTier.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                paymentId = expensePaymentDto.id,
                paymentReferenceNumber = expensePaymentDto.referenceNumber,
                expenseAccountCode = expenseDto.expenseAccountCode,
                payeeContactId = expenseDto.payeeContactId,
                paymentMethodAccountCode = expensePaymentDto.paymentMethodAccountCode,
                amount = expensePaymentDto.amount,
                paymentDate = expensePaymentDto.paymentDate
            )
        )
    }

    fun publishPaymentVoided(
        expensePaymentVoidDto: ExpensePaymentVoidDto,
        expensePaymentDto: ExpensePaymentDto,
        expenseDto: ExpenseDto
    ) {
        applicationEventPublisher.publishEvent(
            ExpensePaymentVoidedEvent(
                eventId = UUID.randomUUID(),
                sourceContext = expenseTier.sourceContext(),
                timestamp = Instant.now(),
                correlationId = null,
                voidId = expensePaymentVoidDto.id,
                paymentId = expensePaymentDto.id,
                paymentReferenceNumber = expensePaymentDto.referenceNumber,
                expenseAccountCode = expenseDto.expenseAccountCode,
                payeeContactId = expenseDto.payeeContactId,
                paymentMethodAccountCode = expensePaymentDto.paymentMethodAccountCode,
                amount = expensePaymentDto.amount,
                voidedOn = DateTimes.Local.atOrganizationZone(expensePaymentVoidDto.voidedOn)
            )
        )
    }
}
