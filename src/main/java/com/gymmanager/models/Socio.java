package com.gymmanager.models;

public class Socio {

    private int     id;
    private String  nombre;
    private String  telefono;
    private String  whatsapp;
    private int     tipoMembresia;
    private String  fechaInicio;
    private String  fechaFin;
    private boolean activo;
    private String  membresiaNombre; // campo transitorio, viene del JOIN

    public Socio() {}

    public int getId()                          { return id; }
    public void setId(int id)                   { this.id = id; }
    public String getNombre()                   { return nombre; }
    public void setNombre(String nombre)        { this.nombre = nombre; }
    public String getTelefono()                 { return telefono; }
    public void setTelefono(String telefono)    { this.telefono = telefono; }
    public String getWhatsapp()                 { return whatsapp; }
    public void setWhatsapp(String whatsapp)    { this.whatsapp = whatsapp; }
    public int getTipoMembresia()               { return tipoMembresia; }
    public void setTipoMembresia(int t)         { this.tipoMembresia = t; }
    public String getFechaInicio()              { return fechaInicio; }
    public void setFechaInicio(String f)        { this.fechaInicio = f; }
    public String getFechaFin()                 { return fechaFin; }
    public void setFechaFin(String f)           { this.fechaFin = f; }
    public boolean isActivo()                   { return activo; }
    public void setActivo(boolean activo)       { this.activo = activo; }
    public String getMembresiaNombre()          { return membresiaNombre; }
    public void setMembresiaNombre(String m)    { this.membresiaNombre = m; }

    @Override
    public String toString() { return nombre; }
}