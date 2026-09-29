package me.ezra_home.retail_software_solution.cucumber.steps.rest

import io.cucumber.java.ParameterType
import me.ezra_home.retail_software_solution.cucumber.support.rest.OrderOption
import me.ezra_home.retail_software_solution.cucumber.support.rest.ParameterStyle
import me.ezra_home.retail_software_solution.cucumber.support.rest.RestVerificationOption
import org.springframework.http.HttpMethod

class RestParameterTypes {

  @ParameterType("GET|POST|PUT|DELETE|PATCH")
  fun httpMethod(raw: String): HttpMethod = HttpMethod.valueOf(raw)

  @ParameterType("\\S+")
  fun path(raw: String): String = raw

  @ParameterType(ParameterStyle.REGEX)
  fun parameterStyle(raw: String): ParameterStyle = ParameterStyle.fromToken(raw)

  @ParameterType(RestVerificationOption.REGEX)
  fun restVerificationOption(raw: String): RestVerificationOption =
    RestVerificationOption.fromToken(raw)

  @ParameterType(OrderOption.REGEX)
  fun orderOption(raw: String): OrderOption = OrderOption.fromToken(raw)
}
