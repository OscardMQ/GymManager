package com.gymmanager.models;

/**
 * Representa una entrada en la bitácora de auditoría del sistema.
 * Registra quién hizo qué y cuándo, para trazabilidad de acciones.
 *
 * Acciones típicas: LOGIN, LOGOUT, SOCIO_CREADO, SOCIO_EDITADO,
 *                   MEMBRESIA_RENOVADA, USUARIO_CREADO, etc.
 */
public class BitacoraEntrada {

    private int    id;
    private String usuario;
    private String fecha;       // yyyy-MM-dd
    private String hora;        // HH:mm:ss
    private String accion;      // Código de acción (ej: "LOGIN", "SOCIO_CREADO")
    private String descripcion; // Detalle legible por humanos

    public BitacoraEntrada() {}

    /** Constructor rápido para registrar una acción sin ID (antes de persistir) */
    public BitacoraEntrada(String usuario, String fecha, String hora,
                           String accion, String descripcion) {
        this.usuario     = usuario;
        this.fecha       = fecha;
        this.hora        = hora;
        this.accion      = accion;
        this.descripcion = descripcion;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public int    getId()          { return id; }
    public void   setId(int id)    { this.id = id; }

    public String getUsuario()           { return usuario; }
    public void   setUsuario(String u)   { this.usuario = u; }

    public String getFecha()             { return fecha; }
    public void   setFecha(String f)     { this.fecha = f; }

    public String getHora()              { return hora; }
    public void   setHora(String h)      { this.hora = h; }

    public String getAccion()            { return accion; }
    public void   setAccion(String a)    { this.accion = a; }

    public String getDescripcion()           { return descripcion; }
    public void   setDescripcion(String d)   { this.descripcion = d; }

    @Override
    public String toString() {
        return "[" + fecha + " " + hora + "] " + usuario + " → " + accion;
    }
}
