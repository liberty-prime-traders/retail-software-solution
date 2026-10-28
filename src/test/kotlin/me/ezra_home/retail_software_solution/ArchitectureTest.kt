package me.ezra_home.retail_software_solution

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes

@AnalyzeClasses(packages = ["me.ezra_home.retail_software_solution"])
class ArchitectureTest {

    @ArchTest
    val domainsShouldBeSelfContained: ArchRule = classes()
        .that(areNonServiceDomainClasses())
        .should(onlyBeAccessedFromOwnDomainOrOutsideBusiness())

    /** A quantity is converted only by multiplying with a captured ratio inside the quantity module. */
    @ArchTest
    val noQuantityMathOnConversionRatioOutsideUtil: ArchRule = classes()
        .that(areOutsideTheQuantityAndBusinessUtilPackages())
        .should(notReadConversionRatioParts())

    /** Rounding policy lives in Decimals: its scales name the policy, a raw RoundingMode call site invents one. */
    @ArchTest
    val noRawBigDecimalRoundingOutsideUtil: ArchRule = classes()
        .that(areOutsideTheQuantityAndBusinessUtilPackages())
        .should(notRoundBigDecimalDirectly())

    private fun notRoundBigDecimalDirectly() = object : ArchCondition<JavaClass>(
        "not call BigDecimal.setScale, BigDecimal.divide or BigDecimal.round with a RoundingMode or MathContext"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            javaClass.methodCallsFromSelf
                .filter { call ->
                    call.targetOwner.name == BIG_DECIMAL_CLASS &&
                        call.target.name in ROUNDING_METHODS &&
                        call.target.rawParameterTypes.any { parameterType -> parameterType.name in ROUNDING_POLICY_CLASSES }
                }
                .forEach { call ->
                    events.add(SimpleConditionEvent.violated(call, "${call.description} rounds directly; use a Decimals scale"))
                }
        }
    }

    private fun areOutsideTheQuantityAndBusinessUtilPackages() = object : DescribedPredicate<JavaClass>("are outside util.quantity and util.business") {
        override fun test(javaClass: JavaClass) =
            !javaClass.packageName.startsWith("$ROOT_PACKAGE.util.quantity") &&
                !javaClass.packageName.startsWith("$ROOT_PACKAGE.util.business")
    }

    private fun notReadConversionRatioParts() = object : ArchCondition<JavaClass>(
        "not call ConversionRatio's numerator, denominator or factor"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            javaClass.methodCallsFromSelf
                .filter { it.targetOwner.name == CONVERSION_RATIO_CLASS && it.target.name in FORBIDDEN_RATIO_MEMBERS }
                .forEach { call ->
                    events.add(SimpleConditionEvent.violated(call, "${call.description} reads a ConversionRatio part outside the quantity module"))
                }
        }
    }

    private fun areNonServiceDomainClasses() = object : DescribedPredicate<JavaClass>("are non-public business domain classes") {
        override fun test(javaClass: JavaClass) =
            domainOf(javaClass.packageName) != null &&
            !javaClass.packageName.contains(".api")
    }

    private fun onlyBeAccessedFromOwnDomainOrOutsideBusiness() = object : ArchCondition<JavaClass>(
        "only be referenced from within the same domain"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            val classDomain = domainOf(javaClass.packageName) ?: return
            val origins = javaClass.accessesToSelf.map { it.originOwner } +
                    javaClass.directDependenciesToSelf.map { it.originClass }
            origins
                .distinct()
                .filter { origin -> domainOf(origin.packageName) != classDomain }
                .forEach { origin ->
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        "${origin.name} (domain: ${domainOf(origin.packageName) ?: "<non-business>"}) " +
                        "references ${javaClass.name} (domain: $classDomain) — go through the .api package"
                    ))
                }
        }
    }

    private companion object {
        const val ROOT_PACKAGE = "me.ezra_home.retail_software_solution"
        const val CONVERSION_RATIO_CLASS = "$ROOT_PACKAGE.util.business.ConversionRatio"
        const val BIG_DECIMAL_CLASS = "java.math.BigDecimal"
        val ROUNDING_POLICY_CLASSES = setOf("java.math.RoundingMode", "java.math.MathContext")
        val ROUNDING_METHODS = setOf("setScale", "divide", "round")
        val FORBIDDEN_RATIO_MEMBERS = setOf(
            "getNumerator", "getDenominator", "component1", "component2", "factor"
        )
    }

    private fun domainOf(packageName: String): String? {
        val idx = packageName.indexOf(".business.")
        if (idx == -1) return null
        val prefix = packageName.substring(0, idx)
        if (prefix.endsWith(".util")) return null
        val afterBusiness = packageName.substring(idx + ".business.".length).substringBefore(".")
        if (afterBusiness.isEmpty()) return null
        return "$prefix.business.$afterBusiness"
    }
}
