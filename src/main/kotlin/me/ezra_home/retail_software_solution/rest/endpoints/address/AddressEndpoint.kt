package me.ezra_home.retail_software_solution.rest.endpoints.address

import me.ezra_home.retail_software_solution.business.address.dto.AddressInsertDto
import me.ezra_home.retail_software_solution.business.address.AddressService
import me.ezra_home.retail_software_solution.business.address.dto.AddressResponseDto
import me.ezra_home.retail_software_solution.business.address.dto.AddressUpdateDto
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("secured/address")
class AddressEndpoint(private val addressService: AddressService) {

    @PostMapping
    fun createAddress(@RequestBody addressInsertDto: AddressInsertDto): AddressResponseDto =
        addressService.createAddress(addressInsertDto)

    @PutMapping
    fun updateAddress(@RequestBody addressDto: AddressUpdateDto): AddressResponseDto =
        addressService.updateAddress(addressDto)

    @GetMapping
    fun getAllAddresses(): Collection<AddressResponseDto> =
        addressService.getAllAddresses()
}
