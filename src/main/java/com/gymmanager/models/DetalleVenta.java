package com.gymmanager.models;

/**
 * Renglón de detalle dentro de una venta.
 * nombreProducto es transitorio: se asigna al armar el carrito en UI,
 * no se persiste en BD.
 */
public class DetalleVenta {

    private int    id;
    private int    ventaId;
    private int    productoId;
    private int    cantidad;
    private double precioUnitario;

    // Transitorio — cargado desde el Producto para mostrar en la tabla del carrito
    private String nombreProducto;

    public DetalleVenta() {}

    public DetalleVenta(int productoId, String nombreProducto,
                        double precioUnitario, int cantidad) {
        this.productoId     = productoId;
        this.nombreProducto = nombreProducto;
        this.precioUnitario = precioUnitario;
        this.cantidad       = cantidad;
    }

    /** Calcula el subtotal de este renglón. */
    public double getSubtotal() {
        return cantidad * precioUnitario;
    }

    public int    getId()                    { return id; }
    public void   setId(int id)              { this.id = id; }

    public int    getVentaId()               { return ventaId; }
    public void   setVentaId(int ventaId)    { this.ventaId = ventaId; }

    public int    getProductoId()            { return productoId; }
    public void   setProductoId(int pid)     { this.productoId = pid; }

    public int    getCantidad()              { return cantidad; }
    public void   setCantidad(int c)         { this.cantidad = c; }

    public double getPrecioUnitario()        { return precioUnitario; }
    public void   setPrecioUnitario(double p){ this.precioUnitario = p; }

    public String getNombreProducto()        { return nombreProducto; }
    public void   setNombreProducto(String n){ this.nombreProducto = n; }
}