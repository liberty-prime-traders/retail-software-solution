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
