package com.yepngo.qrgen.archunit

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.plantuml.rules.PlantUmlArchCondition
import java.net.URL

@AnalyzeClasses(packages = ["com.yepngo.qrgen"])
internal class ArchitectureTest {
    private fun loadResource(filename: String): URL = checkNotNull(javaClass.getResource(filename))

    @ArchTest
    fun packageDependenciesAreOk(tsClasses: JavaClasses) {
        val diagram = loadResource("package-dependencies.puml")
        ArchRuleDefinition
            .classes()
            .should(
                PlantUmlArchCondition.adhereToPlantUmlDiagram(
                    diagram,
                    PlantUmlArchCondition.Configuration.consideringOnlyDependenciesInAnyPackage("com.yepngo.qrgen.."),
                ),
            ).check(tsClasses)
    }
}
