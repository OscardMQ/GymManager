package com.gymmanager.models;

/**
 * Representa un pago registrado al dar de alta o renovar una membresía.
 */
public class Pago {

    private int    id;
    private int    socioId;
    private String fecha;           // ISO-8601: yyyy-MM-dd
    private double monto;
    private int    tipoMembresiaId;

    public Pago() {}

    public int    getId()                       { return id; }
    public void   setId(int id)                 { this.id = id; }
    public int    getSocioId()                  { return socioId; }
    public void   setSocioId(int socioId)       { this.socioId = socioId; }
    public String getFecha()                    { return fecha; }
    public void   setFecha(String fecha)        { this.fecha = fecha; }
    public double getMonto()                    { return monto; }
    public void   setMonto(double monto)        { this.monto = monto; }
    public int    getTipoMembresiaId()          { return tipoMembresiaId; }
    public void   setTipoMembresiaId(int t)     { this.tipoMembresiaId = t; }
}