<p align="center">
  <img src="assets/portadagym.png" alt="GymManager" width="180"/>
</p>

# 💪 GymManager — Gen-Fit

Sistema de escritorio para la gestión integral de un gimnasio: socios, membresías, pagos, punto de venta, inventario, reportes de ganancias y notificaciones por WhatsApp.

Construido con **Java 21 + JavaFX**, usa **SQLite** como base de datos embebida (no necesita servidor) y **BCrypt** para el manejo seguro de contraseñas.

---

## 🚀 Instalación rápida (Windows)

Abre **PowerShell** y ejecuta:

```powershell
irm https://raw.githubusercontent.com/OscardMQ/GymManager/main/install.ps1 | iex
```

El instalador:
1. Verifica que tengas **Java 21+** (si falta, lo instala automáticamente con winget).
2. Descarga la última versión de la app.
3. Crea accesos directos en el **Escritorio** y el **Menú Inicio**.

> También puedes descargar `GymManager.jar` directamente desde [Releases](https://github.com/OscardMQ/GymManager/releases) y ejecutarlo con `java -jar GymManager.jar`.

---

## ✨ Módulos

| Módulo | Descripción |
|---|---|
| 🔐 **Login** | Autenticación con roles (ADMIN / RECEPCIONISTA) y contraseñas BCrypt |
| 📊 **Dashboard** | KPIs en tiempo real: socios activos, vencidos, por vencer, ingresos del mes y stock bajo |
| 👥 **Socios** | Alta, edición, renovación y baja de socios; cálculo automático de vigencia |
| 🎫 **Membresías** | Catálogo configurable (mensual, estudiante, semanal, visita) |
| 💵 **Pagos** | Registro automático al dar de alta o renovar |
| 🛒 **Punto de Venta** | Carrito con validación de stock y descuento automático de inventario (transaccional) |
| 📦 **Productos** | Inventario con alertas de stock bajo por producto |
| 📈 **Reportes** | Ganancias por membresías y ventas, desglose por mes |
| 👤 **Empleados** | Gestión de cuentas de recepcionistas (solo ADMIN) |
| 📱 **Notificaciones** | Aviso diario por WhatsApp al dueño con los socios por vencer y vencidos |
| 📋 **Bitácora** | Registro auditable de todas las acciones del sistema |

El rol **RECEPCIONISTA** tiene acceso restringido: no ve ganancias, reportes, membresías, empleados, bitácora ni notificaciones.

---

## 📱 Notificaciones WhatsApp (CallMeBot)

La app avisa al dueño por WhatsApp cuando hay membresías por vencer (3 días de anticipación) o vencidas. Usa la API gratuita de [CallMeBot](https://www.callmebot.com/blog/free-api-whatsapp-messages/):

1. Agrega el número **+34 684 73 40 44** a tus contactos de WhatsApp.
2. Envíale el mensaje: `allow_callmebot`
3. Te responderá con tu **apikey**.
4. En la app: **Notificaciones → Configuración** → registra tu número y la apikey.
5. Usa el botón **Probar envío** para confirmar que funciona.

> Límite de CallMeBot: ~1 mensaje por minuto. La app envía un solo resumen diario, así que no hay riesgo de bloqueo.

---

## 🛠️ Desarrollo

Requisitos: **JDK 21+** (no necesitas instalar Maven, el proyecto incluye wrapper).

```powershell
git clone https://github.com/OscardMQ/GymManager.git
cd GymManager

# Ejecutar en modo desarrollo
.\mvnw javafx:run

# Generar el JAR ejecutable (queda en target\GymManager.jar)
.\mvnw package
```

### Stack

- **Java 21** · **JavaFX 21** (UI declarativa con FXML + CSS)
- **SQLite** (xerial sqlite-jdbc) — BD embebida con WAL y foreign keys
- **jBCrypt** — hashing de contraseñas con costo 12
- **Maven** — build y empaquetado (shade plugin para el fat JAR)

### Arquitectura

```
src/main/java/com/gymmanager/
├── app/          Punto de entrada (Launcher + Main)
├── controllers/  Controladores JavaFX (uno por vista FXML)
├── services/     Lógica de negocio (Singleton)
├── dao/          Acceso a datos (interfaz + implementación SQLite)
├── models/       Entidades del dominio
├── database/     Conexión Singleton e inicializador de esquema
├── security/     Hashing BCrypt
└── utils/        Utilidades de fechas
```

Los datos se guardan en `~/.gymmanager/gymmanager.db`. La base de datos y las tablas se crean automáticamente en el primer arranque.

---

## 🔑 Primer inicio de sesión

| Usuario | Contraseña | Rol |
|---|---|---|
| `Omar` | `Admin123*` | ADMIN |

> ⚠️ Cambia la contraseña del administrador después del primer inicio de sesión.
