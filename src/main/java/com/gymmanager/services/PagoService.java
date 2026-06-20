package com.gymmanager.services;

import com.gymmanager.dao.PagoDAO;
import com.gymmanager.dao.PagoDAOImpl;
import com.gymmanager.models.Pago;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Lógica de negocio para pagos.
 * Singleton — mismo ciclo de vida que la aplicación.
 */
public class PagoService {

    private static PagoService instancia;
    private final PagoDAO pagoDAO;

    private PagoService() {
        this.pagoDAO = new PagoDAOImpl();
    }

    public static PagoService getInstance() {
        if (instancia == null) instancia = new PagoService();
        return instancia;
    }

    /** Crea y persiste un pago con la fecha de hoy. */
    public void registrarPago(int socioId, double monto, int tipoMembresiaId) throws SQLException {
        Pago pago = new Pago();
        pago.setSocioId       (socioId);
        pago.setFecha         (LocalDate.now().toString());
        pago.setMonto         (monto);
        pago.setTipoMembresiaId(tipoMembresiaId);
        pagoDAO.guardar(pago);
    }

    /** Suma los montos de todos los pagos del mes indicado. */
    public double totalDelMes(int anio, int mes) throws SQLException {
        List<Pago> pagos = pagoDAO.listarDelMes(anio, mes);
        return pagos.stream().mapToDouble(Pago::getMonto).sum();
    }

    /** Atajo: total del mes en curso. */
    public double totalMesActual() throws SQLException {
        LocalDate hoy = LocalDate.now();
        return totalDelMes(hoy.getYear(), hoy.getMonthValue());
    }
}