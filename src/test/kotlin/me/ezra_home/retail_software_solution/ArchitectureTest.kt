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

    private fun areNonServiceDomainClasses() = object : DescribedPredicate<JavaClass>("are non-public business domain classes") {
        override fun test(javaClass: JavaClass) =
            domainOf(javaClass.packageName) != null &&
            !javaClass.packageName.contains(".api")
    }

    private fun onlyBeAccessedFromOwnDomainOrOutsideBusiness() = object : ArchCondition<JavaClass>(
        "only be accessed from within the same domain or outside the business layer"
    ) {
        override fun check(javaClass: JavaClass, events: ConditionEvents) {
            val classDomain = domainOf(javaClass.packageName) ?: return
            javaClass.accessesToSelf
                .filter { access ->
                    val originDomain = domainOf(access.originOwner.packageName)
                    originDomain != null && originDomain != classDomain
                }
                .forEach { access ->
                    events.add(SimpleConditionEvent.violated(
                        javaClass,
                        "${access.originOwner.name} (domain: ${domainOf(access.originOwner.packageName)}) " +
                        "accesses ${javaClass.name} (domain: $classDomain)"
                    ))
                }
        }
    }

    private fun domainOf(packageName: String): String? {
        val idx = packageName.indexOf(".business.")
        if (idx == -1) return null
        val afterBusiness = packageName.substring(idx + ".business.".length).substringBefore(".")
        if (afterBusiness.isEmpty()) return null
        return packageName.substring(0, idx + ".business.".length) + afterBusiness
    }
}
