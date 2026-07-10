package com.gymmanager.utils;

import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.InputStream;

/**
 * Aplica el ícono de la aplicación a las ventanas (principal y modales).
 * Se carga una sola vez de forma perezosa; si el recurso faltara,
 * la ventana simplemente queda con el ícono por defecto de Java.
 */
public final class Iconos {

    private static final String RUTA_ICONO = "/com/gymmanager/images/icono.png";
    private static Image icono;

    public static synchronized void aplicar(Stage stage) {
        if (icono == null) {
            InputStream is = Iconos.class.getResourceAsStream(RUTA_ICONO);
            if (is == null) {
                System.err.println("[Iconos] Recurso no encontrado: " + RUTA_ICONO);
                return;
            }
            icono = new Image(is);
        }
        stage.getIcons().add(icono);
    }

    private Iconos() {}
}
