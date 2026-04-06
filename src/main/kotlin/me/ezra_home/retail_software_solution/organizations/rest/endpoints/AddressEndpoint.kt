package me.ezra_home.retail_software_solution.organizations.rest.endpoints

import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressInsertDto
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressResponseDto
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressService
import me.ezra_home.retail_software_solution.organizations.business.address.api.AddressUpdateDto
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
