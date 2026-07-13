package com.gymmanager.services;

import com.gymmanager.dao.ProductoDAO;
import com.gymmanager.dao.ProductoDAOImpl;
import com.gymmanager.models.Producto;

import java.util.List;

/**
 * Servicio singleton para gestión de productos e inventario.
 * Centraliza validaciones y registra todas las escrituras en bitácora.
 */
public class ProductoService {

    private static ProductoService instancia;

    private final ProductoDAO   productoDAO;
    private final BitacoraService bitacora;

    private ProductoService() {
        this.productoDAO = new ProductoDAOImpl();
        this.bitacora    = BitacoraService.getInstance();
    }

    public static synchronized ProductoService getInstance() {
        if (instancia == null) instancia = new ProductoService();
        return instancia;
    }

    // ─── Escritura ────────────────────────────────────────────────────────────

    /**
     * Da de alta un nuevo producto.
     * @throws Exception si algún campo no cumple las reglas de negocio.
     */
    public void alta(String nombre, String categoria, double precio,
                     int stock, int stockMinimo, String realizadoPor) throws Exception {
        validar(nombre, precio, stock, stockMinimo);

        Producto p = new Producto();
        p.setNombre(nombre.trim());
        p.setCategoria(categoria != null && !categoria.isBlank() ? categoria.trim() : "General");
        p.setPrecio(precio);
        p.setStock(stock);
        p.setStockMinimo(stockMinimo);

        productoDAO.guardar(p);
        bitacora.registrar(realizadoPor, "CREAR_PRODUCTO", "Producto: " + p.getNombre());
    }

    /**
     * Actualiza los datos de un producto existente.
     * @throws Exception si algún campo no cumple las reglas de negocio.
     */
    public void actualizar(Producto producto, String realizadoPor) throws Exception {
        validar(producto.getNombre(), producto.getPrecio(),
                producto.getStock(), producto.getStockMinimo());

        productoDAO.actualizar(producto);
        bitacora.registrar(realizadoPor, "EDITAR_PRODUCTO",
                "Producto ID " + producto.getId() + " → " + producto.getNombre());
    }

    /**
     * Elimina permanentemente un producto del inventario.
     * @throws Exception si el producto tiene ventas registradas (FK) o falla la BD.
     */
    public void eliminar(int id, String realizadoPor) throws Exception {
        try {
            productoDAO.eliminar(id);
        } catch (java.sql.SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("FOREIGN KEY")) {
                throw new Exception("No se puede eliminar: el producto tiene ventas registradas. " +
                        "Puedes dejarlo con stock 0 para que no se venda más.");
            }
            throw e;
        }
        bitacora.registrar(realizadoPor, "ELIMINAR_PRODUCTO", "Producto ID " + id);
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    public List<Producto> listarTodos() {
        return productoDAO.listar();
    }

    /** Productos con stock &lt;= stock_minimo, incluyendo los sin stock. */
    public List<Producto> listarStockBajo() {
        return productoDAO.listarStockBajo();
    }

    // ─── Validaciones ─────────────────────────────────────────────────────────

    private void validar(String nombre, double precio, int stock, int stockMinimo)
            throws Exception {
        if (nombre == null || nombre.isBlank())
            throw new Exception("El nombre del producto es obligatorio.");
        if (precio <= 0)
            throw new Exception("El precio debe ser mayor a cero.");
        if (stock < 0)
            throw new Exception("El stock no puede ser negativo.");
        if (stockMinimo < 0)
            throw new Exception("El stock mínimo no puede ser negativo.");
    }
}