package com.gymmanager.services;

import com.gymmanager.dao.VentaDAO;
import com.gymmanager.dao.VentaDAOImpl;
import com.gymmanager.models.DetalleVenta;
import com.gymmanager.models.Producto;
import com.gymmanager.models.Venta;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Servicio Singleton para el módulo de Punto de Venta.
 *
 * Responsabilidades:
 *   - Validar stock suficiente antes de registrar una venta
 *   - Delegar la persistencia atómica al VentaDAO
 *     (el DAO maneja venta + detalles + descuento de stock en una transacción)
 *   - Registrar la operación en bitácora
 *
 * Usa ProductoService (servicio a servicio) para la validación de stock,
 * respetando la capa de servicios del proyecto.
 */
public class VentaService {

    private static VentaService instancia;

    private final VentaDAO       ventaDAO;
    private final BitacoraService bitacoraService;

    private VentaService() {
        this.ventaDAO        = new VentaDAOImpl();
        this.bitacoraService = BitacoraService.getInstance();
    }

    public static synchronized VentaService getInstance() {
        if (instancia == null) {
            instancia = new VentaService();
        }
        return instancia;
    }

    /**
     * Registra una venta completa:
     *   1. Valida que cada producto tenga stock suficiente.
     *   2. Guarda venta + detalles y descuenta stock (operación atómica).
     *   3. Registra en bitácora.
     *
     * @param carrito  lista de ítems a vender (al menos uno)
     * @param usuario  nombre del usuario que realiza la venta
     * @return Venta persistida con id asignado
     * @throws IllegalStateException si el carrito está vacío o falta stock
     * @throws SQLException          si falla la operación en BD
     */
    public Venta registrarVenta(List<DetalleVenta> carrito, String usuario)
            throws IllegalStateException, SQLException {

        if (carrito == null || carrito.isEmpty()) {
            throw new IllegalStateException("El carrito está vacío.");
        }

        // Cargar stock actual y validar por producto
        List<Producto> productos = ProductoService.getInstance().listarTodos();
        Map<Integer, Producto> mapaProductos = productos.stream()
                .collect(Collectors.toMap(Producto::getId, p -> p));

        for (DetalleVenta detalle : carrito) {
            Producto producto = mapaProductos.get(detalle.getProductoId());
            if (producto == null) {
                throw new IllegalStateException(
                        "Producto no encontrado en inventario (ID: " + detalle.getProductoId() + ")."
                );
            }
            if (producto.getStock() < detalle.getCantidad()) {
                throw new IllegalStateException(
                        "Stock insuficiente para \"" + producto.getNombre() + "\". "
                                + "Disponible: " + producto.getStock()
                                + " — Solicitado: " + detalle.getCantidad() + "."
                );
            }
        }

        // Calcular total sumando subtotales de cada renglón
        double total = carrito.stream()
                .mapToDouble(d -> d.getCantidad() * d.getPrecioUnitario())
                .sum();

        // Construir encabezado de la venta con fecha y hora actuales
        Venta venta = new Venta();
        venta.setFecha(LocalDate.now().toString());
        venta.setHora(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        venta.setUsuario(usuario);
        venta.setTotal(total);

        // Persistir de forma atómica (venta + detalles + stock)
        Venta ventaGuardada = ventaDAO.guardar(venta, carrito);

        // Registrar en bitácora
        String descripcion = String.format(
                "Venta #%d registrada — Total: $%.2f — %d artículo(s)",
                ventaGuardada.getId(), total, carrito.size()
        );
        bitacoraService.registrar(usuario, "VENTA_REGISTRADA", descripcion);

        return ventaGuardada;
    }

    /** Ventas de los últimos {@code dias} días. Delega al DAO. */
    public List<Venta> listarRecientes(int dias) throws SQLException {
        return ventaDAO.listarRecientes(dias);
    }
}