package br.com.attus.processos;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class ArquiteturaHexagonalTest {

    private static final String BASE = "br.com.attus.processos";
    private static final String DOMINIO = BASE + ".domain..";
    private static final String APLICACAO = BASE + ".application..";
    private static final String ADAPTERS = BASE + ".adapter..";
    private static final String ADAPTERS_DE_ENTRADA = BASE + ".adapter.in..";
    private static final String ADAPTERS_DE_SAIDA = BASE + ".adapter.out..";
    private static final String CONFIGURACAO = BASE + ".config..";

    private static JavaClasses producao;

    @BeforeAll
    static void importarClassesDeProducao() {
        producao = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE);
    }

    @Test
    void dominioNaoDependeDeFrameworkNemDeOutrasCamadas() {
        noClasses().that().resideInAPackage(DOMINIO)
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta..", "tools.jackson..", "org.apache.kafka..",
                        APLICACAO, ADAPTERS, CONFIGURACAO)
                .check(producao);
    }

    @Test
    void camadasSoDependemParaDentro() {
        layeredArchitecture().consideringOnlyDependenciesInLayers()
                .layer("Dominio").definedBy(DOMINIO)
                .layer("Aplicacao").definedBy(APLICACAO)
                .layer("Adapters").definedBy(ADAPTERS)
                .layer("Configuracao").definedBy(CONFIGURACAO)
                .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Configuracao")
                .whereLayer("Aplicacao").mayOnlyBeAccessedByLayers("Adapters", "Configuracao")
                .check(producao);
    }

    @Test
    void adaptersDeEntradaNaoConhecemAdaptersDeSaida() {
        noClasses().that().resideInAPackage(ADAPTERS_DE_ENTRADA)
                .should().dependOnClassesThat().resideInAPackage(ADAPTERS_DE_SAIDA)
                .check(producao);
    }

    @Test
    void casosDeUsoNaoSaoComponentesDoSpring() {
        noClasses().that().resideInAPackage(APLICACAO)
                .should().beAnnotatedWith("org.springframework.stereotype.Service")
                .orShould().beAnnotatedWith("org.springframework.stereotype.Component")
                .check(producao);
    }

    @Test
    void adaptersDeEntradaDependemSoDasPortasENaoDasImplementacoes() {
        noClasses().that().resideInAPackage(ADAPTERS_DE_ENTRADA)
                .should().dependOnClassesThat().resideInAPackage(BASE + ".application.usecase..")
                .check(producao);
    }

    @Test
    void controllersFicamNoAdapterWeb() {
        classes().that().areAnnotatedWith("org.springframework.web.bind.annotation.RestController")
                .should().resideInAPackage(BASE + ".adapter.in.web..")
                .andShould().haveSimpleNameEndingWith("Controller")
                .check(producao);
    }
}
