package com.gymmanager.models;

import java.util.List;

/**
 * Encabezado de una venta registrada en el punto de venta.
 * Los detalles son un campo transitorio; no se cargan por defecto.
 */
public class Venta {

    private int    id;
    private String fecha;    // yyyy-MM-dd
    private String hora;     // HH:mm:ss
    private String usuario;
    private double total;

    // Campo transitorio — útil si se necesita mostrar el desglose
    private List<DetalleVenta> detalles;

    public Venta() {}

    public Venta(String fecha, String hora, String usuario, double total) {
        this.fecha   = fecha;
        this.hora    = hora;
        this.usuario = usuario;
        this.total   = total;
    }

    public int    getId()      { return id; }
    public void   setId(int id){ this.id = id; }

    public String getFecha()            { return fecha; }
    public void   setFecha(String f)    { this.fecha = f; }

    public String getHora()             { return hora; }
    public void   setHora(String h)     { this.hora = h; }

    public String getUsuario()          { return usuario; }
    public void   setUsuario(String u)  { this.usuario = u; }

    public double getTotal()            { return total; }
    public void   setTotal(double t)    { this.total = t; }

    public List<DetalleVenta> getDetalles()            { return detalles; }
    public void               setDetalles(List<DetalleVenta> d) { this.detalles = d; }

    @Override
    public String toString() {
        return String.format("Venta{id=%d, fecha=%s, total=%.2f}", id, fecha, total);
    }
}