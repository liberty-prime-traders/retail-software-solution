package me.ezra_home.retail_software_solution.util.exceptions

internal class RtsMissingHeaderException(headerName: String): RtsGenericException(
    "Header $headerName is required to access this resource"
)
