package com.gymmanager.app;

import javafx.application.Application;

/**
 * Punto de entrada real del JAR.
 * Existe porque la JVM verifica si el main() extiende Application antes de
 * cargar los módulos de JavaFX. Al separar Launcher de Main, ese chequeo
 * no falla. Se usa la forma explícita Application.launch(Main.class, args)
 * para que JavaFX sepa exactamente qué clase inicializar.
 */
public class Launcher {

    public static void main(String[] args) {
        Application.launch(Main.class, args); // forma explícita: evita la detección por reflection
    }
}