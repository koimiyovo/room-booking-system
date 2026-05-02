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
    fun `domain does not depend on infrastructure`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.domain..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.kyovo.infrastructure..")
            .check(importedClasses)
    }

    @Test
    fun `infrastructure-api does not depend on infrastructure-persistence`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.infrastructure.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.kyovo.infrastructure.persistence..")
            .check(importedClasses)
    }

    @Test
    fun `infrastructure-persistence does not depend on infrastructure-api`()
    {
        noClasses()
            .that().resideInAPackage("com.kyovo.infrastructure.persistence..")
            .should().dependOnClassesThat()
            .resideInAPackage("com.kyovo.infrastructure.api..")
            .check(importedClasses)
    }

    @Test
    fun `persistence adapters follow naming convention`()
    {
        classes()
            .that().resideInAPackage("com.kyovo.infrastructure.persistence.adapter..")
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
