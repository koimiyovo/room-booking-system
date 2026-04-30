package com.kyovo

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test

class ArchitectureTest
{
    private val importedClasses = ClassFileImporter().importPackages("com.kyovo")

    @Test
    fun `domain is Spring free`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("org.springframework..")
            .check(importedClasses)
    }

    @Test
    fun `domain is JPA free`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("jakarta.persistence..")
            .check(importedClasses)
    }

    @Test
    fun `domain does not depend on adapters`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.kyovo.adapter..")
            .check(importedClasses)
    }

    @Test
    fun `adapter-web does not depend on adapter-persistence`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.adapter.web..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.kyovo.adapter.persistence..")
            .check(importedClasses)
    }

    @Test
    fun `adapter-persistence does not depend on adapter-web`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.adapter.persistence..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.kyovo.adapter.web..")
            .check(importedClasses)
    }

    @Test
    fun `persistence adapters follow naming convention`()
    {
        classes()
            .that().resideInAPackage("com.kyovo.adapter.persistence.adapter..")
            .should().haveSimpleNameEndingWith("Adapter")
            .check(importedClasses)
    }

    @Test
    fun `no cyclic dependencies in domain`()
    {
        slices()
            .matching("com.kyovo.domain.(*)..")
            .should().beFreeOfCycles()
            .check(importedClasses)
    }
}
