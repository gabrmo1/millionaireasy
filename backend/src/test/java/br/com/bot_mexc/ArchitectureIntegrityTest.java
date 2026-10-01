package br.com.bot_mexc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureIntegrityTest {

    @Test
    @DisplayName("Spring Modulith deve validar a integridade estrutural e fronteiras dos módulos")
    void verificarIntegridadeDosModulos() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        modules.forEach(System.out::println);
        modules.verify();
    }
}
