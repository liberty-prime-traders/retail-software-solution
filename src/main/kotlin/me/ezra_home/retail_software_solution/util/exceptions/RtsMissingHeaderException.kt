package me.ezra_home.retail_software_solution.util.exceptions

class RtsMissingHeaderException(headerName: String): RtsGenericException(
    "Missing Request Header: $headerName"
)
