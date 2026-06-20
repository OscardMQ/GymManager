package com.gymmanager.security;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para hashing y verificación de contraseñas con BCrypt.
 *
 * Factor de costo 12: buen balance entre seguridad y velocidad
 * (~300 ms por hash en hardware moderno, lo que dificulta ataques de fuerza bruta).
 *
 * NUNCA almacenar contraseñas en texto plano; siempre pasar por esta clase.
 */
public class PasswordHasher {

    /** Costo de BCrypt. Cada punto extra duplica el tiempo de cómputo. */
    private static final int COSTO = 12;

    /**
     * Genera un hash BCrypt de la contraseña.
     * El salt queda embebido en el hash resultante.
     *
     * @param contrasena Contraseña en texto plano (nunca persistir esto)
     * @return Hash BCrypt listo para guardar en BD
     */
    public static String hashear(String contrasena) {
        return BCrypt.hashpw(contrasena, BCrypt.gensalt(COSTO));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash BCrypt almacenado.
     *
     * @param contrasena Texto plano ingresado por el usuario
     * @param hash       Hash BCrypt recuperado de la base de datos
     * @return true si la contraseña es correcta
     */
    public static boolean verificar(String contrasena, String hash) {
        try {
            return BCrypt.checkpw(contrasena, hash);
        } catch (Exception e) {
            // Captura IllegalArgumentException si el hash está malformado
            return false;
        }
    }

    /** Constructor privado: clase de utilidad estática, no instanciar */
    private PasswordHasher() {}
}
