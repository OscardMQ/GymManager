package com.gymmanager.app;

import com.gymmanager.controllers.LoginController;
import com.gymmanager.database.DatabaseInitializer;
import com.gymmanager.services.NotificacionService;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicación JavaFX.
 * init() se ejecuta antes de que aparezca la ventana, ideal para
 * inicializar BD y lanzar tareas de fondo.
 */
public class Main extends Application {

    /**
     * Se ejecuta en el hilo del launcher (antes de start()).
     * Inicializa la BD y arranca la verificación de notificaciones
     * en un hilo daemon para no bloquear el arranque.
     */
    @Override
    public void init() {
        // Crea tablas y datos semilla si no existen
        DatabaseInitializer.inicializar();

        // Verifica vencimientos al iniciar; falla silenciosamente si no hay red/config
        Thread t = new Thread(
                () -> NotificacionService.getInstance().verificarYNotificarVencimientos(),
                "verificacion-notificaciones-inicio"
        );
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/com/gymmanager/views/login.fxml"));
        Scene scene = new Scene(loader.load());

        primaryStage.setTitle("GymManager — Gen Fit");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(650);
        primaryStage.show();
    }
}