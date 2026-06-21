package com.gymmanager.models;

/**
 * Modelo del inventario del gimnasio.
 * El estado de stock se calcula en tiempo real, no se persiste.
 */
public class Producto {

    public enum EstadoStock { OK, STOCK_BAJO, SIN_STOCK }

    private int    id;
    private String nombre;
    private double precio;
    private int    stock;
    private int    stockMinimo;
    private String categoria;

    public Producto() {}

    public Producto(int id, String nombre, double precio,
                    int stock, int stockMinimo, String categoria) {
        this.id          = id;
        this.nombre      = nombre;
        this.precio      = precio;
        this.stock       = stock;
        this.stockMinimo = stockMinimo;
        this.categoria   = categoria;
    }

    // ─── Lógica de negocio ────────────────────────────────────────────────────

    /** Compara stock actual contra mínimo para determinar el estado. */
    public EstadoStock getEstadoStock() {
        if (stock == 0)            return EstadoStock.SIN_STOCK;
        if (stock <= stockMinimo)  return EstadoStock.STOCK_BAJO;
        return EstadoStock.OK;
    }

    public String getEstadoStockTexto() {
        return switch (getEstadoStock()) {
            case SIN_STOCK  -> "Sin stock";
            case STOCK_BAJO -> "Stock bajo";
            case OK         -> "OK";
        };
    }

    public String getPrecioFormateado() {
        return String.format("$%.2f", precio);
    }

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public int    getId()           { return id; }
    public void   setId(int id)     { this.id = id; }

    public String getNombre()              { return nombre; }
    public void   setNombre(String n)      { this.nombre = n; }

    public double getPrecio()              { return precio; }
    public void   setPrecio(double p)      { this.precio = p; }

    public int    getStock()               { return stock; }
    public void   setStock(int s)          { this.stock = s; }

    public int    getStockMinimo()         { return stockMinimo; }
    public void   setStockMinimo(int sm)   { this.stockMinimo = sm; }

    public String getCategoria()           { return categoria; }
    public void   setCategoria(String c)   { this.categoria = c; }
}