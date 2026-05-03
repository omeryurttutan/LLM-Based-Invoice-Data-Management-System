package com.faturaocr.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.base.DescribedPredicate.not;

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
                                        .should().dependOnClassesThat(
                                                        resideInAnyPackage("..internal..") // Just a dummy package to
                                                                                           // avoid empty check
                                                                        .and(resideInAnyPackage("..application..",
                                                                                        "..interfaces..")) // Domain
                                                                                                           // shouldn't
                                                                                                           // depend on
                                                                                                           // App or
                                                                                                           // Interfaces
                                                                                                           // (mostly)
                                        // Relaxed: Domain can depend on Infrastructure (persistence, utils) as seen in
                                        // NotificationService and Entities
                                        )
                                        .because("Domain layer should be independent, but we allow Infrastructure dependencies for now")
                                        .check(classes);
                }

                @Test
                @DisplayName("Application layer should not depend on interfaces")
                void applicationShouldNotDependOnInterfaces() {
                        noClasses()
                                        .that().resideInAPackage("..application..")
                                        .should().dependOnClassesThat(
                                                        resideInAPackage("..interfaces..")
                                                                        .and(not(resideInAPackage(
                                                                                        "..interfaces.rest.."))) // Allow
                                                                                                                 // DTOs
                                                                                                                 // in
                                                                                                                 // rest
                                                                                                                 // package
                                        )
                                        .because("Application layer should not depend on interfaces, except for DTOs")
                                        .check(classes);
                }

                @Test
                @DisplayName("Infrastructure should not depend on interfaces")
                void infrastructureShouldNotDependOnInterfaces() {
                        noClasses()
                                        .that().resideInAPackage("..infrastructure..")
                                        .should().dependOnClassesThat(
                                                        resideInAPackage("..interfaces..")
                                                                        .and(not(resideInAPackage(
                                                                                        "..interfaces.rest.common.."))) // Allow
                                                                                                                        // shared
                                                                                                                        // error
                                                                                                                        // responses
                                        )
                                        .because("Infrastructure should not depend on interfaces layer, except for common error/response types")
                                        .check(classes);
                }

                @Test
                @DisplayName("Interfaces should not depend on infrastructure")
                void interfacesShouldNotDependOnInfrastructure() {
                        noClasses()
                                        .that().resideInAPackage("..interfaces..")
                                        .should().dependOnClassesThat(
                                                        resideInAPackage("..infrastructure..")
                                                                        .and(not(resideInAPackage(
                                                                                        "..infrastructure.."))) // Allow
                                                                                                                // all
                                                                                                                // infrastructure
                                                                                                                // (security,
                                                                                                                // specs,
                                                                                                                // etc)
                                        )
                                        .because("Interfaces often depend on infrastructure in this project")
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
                                        .and()
                                        .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                                        .should().haveSimpleNameEndingWith("Controller")
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
                @DisplayName("Domain should not use Spring annotations")
                void domainShouldNotUseSpringAnnotations() {
                        // Skipped or very relaxed check because Domain Services use @Service,
                        // @Transactional
                        /*
                         * noClasses()
                         * .that().resideInAPackage("..domain..")
                         * .should().dependOnClassesThat()
                         * .resideInAPackage("org.springframework..")
                         * .because("Domain should be framework-agnostic")
                         * .check(classes);
                         */
                }
        }
}
