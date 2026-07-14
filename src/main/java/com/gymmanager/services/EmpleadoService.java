package com.gymmanager.services;

import com.gymmanager.dao.UsuarioDAO;
import com.gymmanager.dao.UsuarioDAOImpl;
import com.gymmanager.models.Usuario;
import com.gymmanager.security.PasswordHasher;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Servicio singleton para gestión de empleados con rol RECEPCIONISTA.
 * Centraliza validaciones de negocio y delega persistencia al UsuarioDAO.
 * Registra todas las operaciones de escritura en la bitácora.
 */
public class EmpleadoService {

    private static EmpleadoService instancia;

    private final UsuarioDAO usuarioDAO;
    private final BitacoraService bitacora;

    private EmpleadoService() {
        this.usuarioDAO = new UsuarioDAOImpl();
        this.bitacora   = BitacoraService.getInstance();
    }

    public static synchronized EmpleadoService getInstance() {
        if (instancia == null) instancia = new EmpleadoService();
        return instancia;
    }

    // ─── Operaciones de escritura ─────────────────────────────────────────────

    /**
     * Crea un nuevo empleado con rol RECEPCIONISTA.
     * Valida que el nombre de usuario sea único y que la contraseña
     * cumpla el mínimo de 8 caracteres.
     *
     * @param usuario      Nombre de usuario (login).
     * @param contrasena   Contraseña en texto plano; se hashea antes de guardar.
     * @param realizadoPor Usuario en sesión, para la bitácora.
     * @throws Exception si la validación falla o el usuario ya existe.
     */
    public void crearEmpleado(String usuario, String contrasena, String realizadoPor)
            throws Exception {

        validarCamposNuevo(usuario, contrasena);

        if (usuarioDAO.buscarPorNombre(usuario.trim()).isPresent()) {
            throw new Exception("El usuario «" + usuario.trim() + "» ya está en uso.");
        }

        Usuario nuevo = new Usuario();
        nuevo.setUsuario(usuario.trim());
        nuevo.setContrasenaBcrypt(PasswordHasher.hashear(contrasena));
        nuevo.setRol(Usuario.Rol.RECEPCIONISTA);
        nuevo.setActivo(true);

        usuarioDAO.guardar(nuevo);
        bitacora.registrar(realizadoPor, "CREAR_EMPLEADO",
                "Empleado creado: " + usuario.trim());
    }

    /**
     * Actualiza el nombre de usuario y/o la contraseña de un empleado existente.
     * Si la nueva contraseña está vacía o nula, se conserva la actual.
     *
     * @param id              ID del empleado a editar.
     * @param nuevoUsuario    Nuevo nombre de usuario.
     * @param nuevaContrasena Nueva contraseña en texto plano, o null/vacío para no cambiar.
     * @param realizadoPor    Usuario en sesión, para la bitácora.
     * @throws Exception si la validación falla o el nombre ya lo tiene otro empleado.
     */
    public void editarEmpleado(int id, String nuevoUsuario, String nuevaContrasena,
                               String realizadoPor) throws Exception {

        if (nuevoUsuario == null || nuevoUsuario.isBlank()) {
            throw new Exception("El nombre de usuario es obligatorio.");
        }

        // Verificar que el nombre no esté ocupado por otro usuario distinto
        Optional<Usuario> existente = usuarioDAO.buscarPorNombre(nuevoUsuario.trim());
        if (existente.isPresent() && existente.get().getId() != id) {
            throw new Exception("El usuario «" + nuevoUsuario.trim() + "» ya está en uso.");
        }

        usuarioDAO.actualizarNombreUsuario(id, nuevoUsuario.trim());

        if (nuevaContrasena != null && !nuevaContrasena.isBlank()) {
            if (nuevaContrasena.length() < 8) {
                throw new Exception("La contraseña debe tener al menos 8 caracteres.");
            }
            usuarioDAO.actualizarContrasena(id, PasswordHasher.hashear(nuevaContrasena));
        }

        bitacora.registrar(realizadoPor, "EDITAR_EMPLEADO",
                "Empleado ID " + id + " actualizado → " + nuevoUsuario.trim());
    }

    /**
     * Activa o desactiva un empleado.
     *
     * @param id           ID del empleado.
     * @param activo       true para activar, false para desactivar.
     * @param realizadoPor Usuario en sesión, para la bitácora.
     * @throws Exception si falla la operación en base de datos.
     */
    public void cambiarEstado(int id, boolean activo, String realizadoPor) throws Exception {
        try {
            usuarioDAO.cambiarEstado(id, activo);
        } catch (SQLException e) {
            throw new Exception("Error al cambiar estado del empleado: " + e.getMessage(), e);
        }
        String accion = activo ? "ACTIVAR_EMPLEADO" : "DESACTIVAR_EMPLEADO";
        bitacora.registrar(realizadoPor, accion,
                "Empleado ID " + id + " → " + (activo ? "activo" : "inactivo"));
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    /** Devuelve todos los usuarios con rol RECEPCIONISTA, ordenados por nombre. */
    public List<Usuario> listarEmpleados() {
        return usuarioDAO.listarPorRol(Usuario.Rol.RECEPCIONISTA);
    }

    // ─── Validaciones internas ────────────────────────────────────────────────

    private void validarCamposNuevo(String usuario, String contrasena) throws Exception {
        if (usuario == null || usuario.isBlank())
            throw new Exception("El nombre de usuario es obligatorio.");
        if (contrasena == null || contrasena.isBlank())
            throw new Exception("La contraseña es obligatoria.");
        if (contrasena.length() < 8)
            throw new Exception("La contraseña debe tener al menos 8 caracteres.");
    }
}