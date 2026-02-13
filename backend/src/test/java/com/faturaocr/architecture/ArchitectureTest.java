package com.faturaocr.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Architecture tests to enforce hexagonal architecture rules.
 */
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.faturaocr");
    }

    @Nested
    @DisplayName("Layer Dependency Rules")
    class LayerDependencyTests {

        @Test
        @DisplayName("Domain layer should not depend on other layers")
        void domainShouldNotDependOnOtherLayers() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..application..", "..infrastructure..", "..interfaces..")
                    .because("Domain layer should be independent of other layers")
                    .check(classes);
        }

        @Test
        @DisplayName("Application layer should not depend on infrastructure or interfaces")
        void applicationShouldNotDependOnInfrastructureOrInterfaces() {
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("..infrastructure..", "..interfaces..")
                    .because("Application layer should only depend on domain")
                    .check(classes);
        }

        @Test
        @DisplayName("Infrastructure should not depend on interfaces")
        void infrastructureShouldNotDependOnInterfaces() {
            noClasses()
                    .that().resideInAPackage("..infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..interfaces..")
                    .because("Infrastructure should not depend on interfaces layer")
                    .check(classes);
        }

        @Test
        @DisplayName("Interfaces should not depend on infrastructure")
        void interfacesShouldNotDependOnInfrastructure() {
            noClasses()
                    .that().resideInAPackage("..interfaces..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Interfaces should not depend on infrastructure layer")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Naming Conventions")
    class NamingConventionTests {

        @Test
        @DisplayName("Controllers should be suffixed with Controller")
        void controllersShouldBeSuffixed() {
            classes()
                    .that().resideInAPackage("..interfaces.rest..")
                    .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .check(classes);
        }

        @Test
        @DisplayName("Use cases should be suffixed with UseCase")
        void useCasesShouldBeSuffixed() {
            classes()
                    .that().resideInAPackage("..application..usecase..")
                    .and().areNotInterfaces()
                    .should().haveSimpleNameEndingWith("UseCase")
                    .check(classes);
        }

        @Test
        @DisplayName("Repository adapters should be suffixed with RepositoryAdapter")
        void repositoryAdaptersShouldBeSuffixed() {
            classes()
                    .that().resideInAPackage("..infrastructure.persistence..")
                    .and().haveSimpleNameContaining("Adapter")
                    .should().haveSimpleNameEndingWith("RepositoryAdapter")
                    .check(classes);
        }
    }

    @Nested
    @DisplayName("Domain Rules")
    class DomainRules {

        @Test
        @DisplayName("Domain entities should extend BaseEntity")
        void entitiesShouldExtendBaseEntity() {
            classes()
                    .that().resideInAPackage("..domain..entity..")
                    .and().areNotInterfaces()
                    .and().doNotHaveSimpleName("BaseEntity")
                    .should().beAssignableTo(com.faturaocr.domain.common.entity.BaseEntity.class)
                    .check(classes);
        }

        @Test
        @DisplayName("Domain should not use Spring annotations")
        void domainShouldNotUseSpringAnnotations() {
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("org.springframework..")
                    .because("Domain should be framework-agnostic")
                    .check(classes);
        }
    }
}
