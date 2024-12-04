package me.ezra_home.retail_software_solution.rest.endpoints.address

import me.ezra_home.retail_software_solution.business.address.AddressDto
import me.ezra_home.retail_software_solution.business.address.AddressService
import me.ezra_home.retail_software_solution.business.address.AddressWithAudit
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@CrossOrigin
@RestController
@RequestMapping("/secured/address")
class AddressEndpoint(private val addressService: AddressService) {

    @GetMapping
    fun getAllAddresses(): Collection<AddressWithAudit> = addressService.getAllAddresses()

    @PostMapping
    fun createAddress(@RequestBody addressDto: AddressDto): AddressDto =
        addressService.createAddress(addressDto)

    @PutMapping
    fun updateAddress(@RequestBody addressDto: AddressDto): AddressDto =
        addressService.updateAddress(addressDto)

}
