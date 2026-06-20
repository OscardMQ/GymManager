package com.gymmanager.models;

/**
 * Encapsula las credenciales capturadas en el formulario de login.
 * Inmutable por diseño — no se modifican después de crearse.
 */
public class LoginRequest {

    private final String usuario;
    private final String contrasena;

    public LoginRequest(String usuario, String contrasena) {
        this.usuario = usuario;
        this.contrasena = contrasena;
    }

    public String getUsuario()    { return usuario; }
    public String getContrasena() { return contrasena; }
}