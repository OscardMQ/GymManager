package com.gymmanager.models;

public class Membresia {

    private int    id;
    private String nombre;
    private double precio;
    private int    duracionDias;
    private double descuentoEstudiante;
    private String descripcion;

    public Membresia() {}

    public int getId()                              { return id; }
    public void setId(int id)                       { this.id = id; }
    public String getNombre()                       { return nombre; }
    public void setNombre(String nombre)            { this.nombre = nombre; }
    public double getPrecio()                       { return precio; }
    public void setPrecio(double precio)            { this.precio = precio; }
    public int getDuracionDias()                    { return duracionDias; }
    public void setDuracionDias(int d)              { this.duracionDias = d; }
    public double getDescuentoEstudiante()          { return descuentoEstudiante; }
    public void setDescuentoEstudiante(double d)    { this.descuentoEstudiante = d; }
    public String getDescripcion()                  { return descripcion; }
    public void setDescripcion(String descripcion)  { this.descripcion = descripcion; }

    @Override
    public String toString() {
        return String.format("%s — $%.0f (%d días)", nombre, precio, duracionDias);
    }
}