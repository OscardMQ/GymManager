package com.gymmanager.models;

/**
 * Representa un usuario del sistema (administrador o recepcionista).
 * El campo contrasenaBcrypt nunca debe exponerse en la UI.
 */
public class Usuario {

    public enum Rol { ADMIN, RECEPCIONISTA }

    private int    id;
    private String usuario;
    private String contrasenaBcrypt; // Solo para operaciones internas, nunca mostrar
    private Rol    rol;
    private boolean activo;

    public Usuario() {}

    public Usuario(int id, String usuario, String contrasenaBcrypt, Rol rol, boolean activo) {
        this.id               = id;
        this.usuario          = usuario;
        this.contrasenaBcrypt = contrasenaBcrypt;
        this.rol              = rol;
        this.activo           = activo;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int     getId()               { return id; }
    public void    setId(int id)         { this.id = id; }

    public String  getUsuario()                { return usuario; }
    public void    setUsuario(String usuario)  { this.usuario = usuario; }

    public String  getContrasenaBcrypt()                       { return contrasenaBcrypt; }
    public void    setContrasenaBcrypt(String contrasenaBcrypt){ this.contrasenaBcrypt = contrasenaBcrypt; }

    public Rol     getRol()          { return rol; }
    public void    setRol(Rol rol)   { this.rol = rol; }

    public boolean isActivo()            { return activo; }
    public void    setActivo(boolean a)  { this.activo = a; }

    /** Verificación de rol rápida para control de acceso en la UI */
    public boolean esAdmin()         { return Rol.ADMIN == rol; }
    public boolean esRecepcionista() { return Rol.RECEPCIONISTA == rol; }

    @Override
    public String toString() {
        return "Usuario{id=" + id + ", usuario='" + usuario + "', rol=" + rol + "}";
    }
}
