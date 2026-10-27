package me.ezra_home.retail_software_solution.util.exceptions

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import me.ezra_home.retail_software_solution.util.business.Money
import me.ezra_home.retail_software_solution.util.business.Percentage
import me.ezra_home.retail_software_solution.util.quantity.UserProvidedQuantity
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

class GlobalExceptionHandlerBindingTest {

    data class QuantityRequest(val quantity: UserProvidedQuantity)

    @RestController
    class QuantityController {
        @PostMapping("/quantities")
        fun accept(@RequestBody quantityRequest: QuantityRequest): BigDecimal = quantityRequest.quantity.toBigDecimal()
    }

    data class AmountRequest(val amount: Money)

    data class RateRequest(val rate: Percentage)

    @RestController
    class AmountController {
        @PostMapping("/amounts")
        fun accept(@RequestBody amountRequest: AmountRequest): BigDecimal = amountRequest.amount.toBigDecimal()
    }

    @RestController
    class RateController {
        @PostMapping("/rates")
        fun accept(@RequestBody rateRequest: RateRequest): BigDecimal = rateRequest.rate.toBigDecimal()
    }

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(QuantityController(), AmountController(), RateController())
            .setControllerAdvice(GlobalExceptionHandler())
            .setMessageConverters(MappingJackson2HttpMessageConverter(jacksonObjectMapper().findAndRegisterModules()))
            .build()
    }

    @Test
    fun `a quantity with three decimals returns a business error, not a 500`() {
        mockMvc.perform(post("/quantities").contentType(MediaType.APPLICATION_JSON).content("""{"quantity": 1.255}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Quantity can have at most 2 decimal places"))
    }

    @Test
    fun `a quantity written with trailing zeros is accepted`() {
        mockMvc.perform(post("/quantities").contentType(MediaType.APPLICATION_JSON).content("""{"quantity": 1.2500}"""))
            .andExpect(status().isOk)
    }

    @Test
    fun `an unreadable body is a 400 with a generic message`() {
        mockMvc.perform(post("/quantities").contentType(MediaType.APPLICATION_JSON).content("""{"quantity": """))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("The request body could not be read"))
    }

    @Test
    fun `a money amount with three decimals returns a business error, not a 500`() {
        mockMvc.perform(post("/amounts").contentType(MediaType.APPLICATION_JSON).content("""{"amount": 1.255}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Amount can have at most 2 decimal places"))
    }

    @Test
    fun `a negative money amount returns a business error, not a 500`() {
        mockMvc.perform(post("/amounts").contentType(MediaType.APPLICATION_JSON).content("""{"amount": -1}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Amount must not be negative"))
    }

    @Test
    fun `a percentage above one hundred returns a business error, not a 500`() {
        mockMvc.perform(post("/rates").contentType(MediaType.APPLICATION_JSON).content("""{"rate": 100.01}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Percentage must not exceed 100"))
    }

    @Test
    fun `a percentage with three decimals returns a business error, not a 500`() {
        mockMvc.perform(post("/rates").contentType(MediaType.APPLICATION_JSON).content("""{"rate": 7.125}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Percentage can have at most 2 decimal places"))
    }
}
