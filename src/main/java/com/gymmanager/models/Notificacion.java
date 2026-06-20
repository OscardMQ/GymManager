package com.gymmanager.models;

/**
 * Registro de un intento de notificación WhatsApp (enviado o fallido).
 * El campo nombreSocio se resuelve con JOIN en la consulta; no se persiste.
 */
public class Notificacion {

    private int    id;
    private int    socioId;
    private String tipo;        // VENCIMIENTO | RESUMEN_DUENO | PRUEBA
    private String mensaje;
    private String fechaEnvio;  // yyyy-MM-dd HH:mm:ss
    private String estado;      // ENVIADO | ERROR | SIN_WHATSAPP
    private String nombreSocio; // transiente, viene del JOIN

    public Notificacion() {}

    // ── Getters ──────────────────────────────────────────────────────────────

    public int    getId()          { return id; }
    public int    getSocioId()     { return socioId; }
    public String getTipo()        { return tipo; }
    public String getMensaje()     { return mensaje; }
    public String getFechaEnvio()  { return fechaEnvio; }
    public String getEstado()      { return estado; }

    /** Si no hay socio asociado (ej: resumen al dueño), devuelve "Sistema". */
    public String getNombreSocio() { return nombreSocio != null ? nombreSocio : "Sistema"; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setId(int id)                { this.id = id; }
    public void setSocioId(int socioId)      { this.socioId = socioId; }
    public void setTipo(String tipo)         { this.tipo = tipo; }
    public void setMensaje(String mensaje)   { this.mensaje = mensaje; }
    public void setFechaEnvio(String f)      { this.fechaEnvio = f; }
    public void setEstado(String estado)     { this.estado = estado; }
    public void setNombreSocio(String n)     { this.nombreSocio = n; }
}