package com.gymmanager.dao;

import com.gymmanager.models.Producto;
import java.util.List;
import java.util.Optional;

/**
 * Contrato de persistencia para productos del inventario.
 */
public interface ProductoDAO {
    List<Producto>    listar();
    Optional<Producto> buscarPorId(int id);
    void              guardar(Producto producto);
    void              actualizar(Producto producto);
    void              eliminar(int id);
    /** Retorna productos donde stock &lt;= stock_minimo, ordenados por stock ASC. */
    List<Producto>    listarStockBajo();
}