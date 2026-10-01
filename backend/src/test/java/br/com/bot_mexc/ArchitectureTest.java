package br.com.bot_mexc;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ArchitectureTest {

    @Test
    void printModules() {
        ApplicationModules modules = ApplicationModules.of(Application.class);
        System.out.println("=== MODULOS DETECTADOS PELO SPRING MODULITH ===");
        modules.forEach(System.out::println);
    }
}
