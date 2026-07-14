package com.gymmanager.services;

import com.gymmanager.database.DatabaseConnection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * Recuperación de contraseña del administrador sin puerta trasera.
 *
 * El código se genera en memoria y se escribe SOLO en un archivo dentro
 * de la carpeta de datos local (~/.gymmanager). Nunca se muestra en pantalla:
 * para completar la recuperación hay que poder abrir ese archivo, lo que
 * exige acceso físico a la computadora del gimnasio. Un atacante remoto
 * (o alguien mirando la pantalla) no puede obtenerlo.
 *
 * El código vive en memoria: al cerrar la app queda invalidado, así que un
 * archivo viejo por sí solo no sirve. Vigencia de 10 minutos y un solo uso.
 */
public class RecuperacionService {

    private static final String   ARCHIVO  = "recuperacion.txt";
    private static final Duration VIGENCIA = Duration.ofMinutes(10);
    private static final int      LONGITUD = 8;
    // Sin caracteres ambiguos (O/0, I/1/L) para que sea fácil de copiar
    private static final String   ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    private static RecuperacionService instancia;

    private final SecureRandom random = new SecureRandom();
    private String  codigoActual;
    private Instant generadoEn;

    private RecuperacionService() {}

    public static synchronized RecuperacionService getInstance() {
        if (instancia == null) instancia = new RecuperacionService();
        return instancia;
    }

    /**
     * Genera un código nuevo, lo escribe en el archivo de datos local
     * y devuelve su ruta para mostrársela al usuario.
     */
    public synchronized Path generarCodigo() throws IOException {
        StringBuilder sb = new StringBuilder(LONGITUD);
        for (int i = 0; i < LONGITUD; i++) {
            sb.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        codigoActual = sb.toString();
        generadoEn   = Instant.now();

        Path archivo = rutaArchivo();
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, """
                GymManager — Codigo de recuperacion de contrasena
                =================================================

                Tu codigo es:   %s

                Escribelo en la ventana de recuperacion de la aplicacion.
                Valido por 10 minutos y un solo uso. No lo compartas con nadie.
                """.formatted(codigoActual));
        return archivo;
    }

    /** true si el código ingresado coincide y no ha expirado. */
    public synchronized boolean validar(String codigoIngresado) {
        if (codigoActual == null || generadoEn == null || codigoIngresado == null) return false;
        if (Instant.now().isAfter(generadoEn.plus(VIGENCIA))) return false;
        return codigoActual.equalsIgnoreCase(codigoIngresado.trim());
    }

    /** Invalida el código en memoria y borra el archivo tras un reset exitoso. */
    public synchronized void limpiar() {
        codigoActual = null;
        generadoEn   = null;
        try {
            Files.deleteIfExists(rutaArchivo());
        } catch (IOException e) {
            System.err.println("[RecuperacionService] No se pudo borrar el archivo: " + e.getMessage());
        }
    }

    /** Ruta del archivo de recuperación dentro de la carpeta de datos. */
    public Path rutaArchivo() {
        return DatabaseConnection.getRutaBaseDatos().getParent().resolve(ARCHIVO);
    }
}
