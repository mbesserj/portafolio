package com.ui.App;

import com.model.utiles.LibraryInitializer;
import javafx.application.Application;

public class AppLauncher {

    public static void main(String[] args) {

        /**
         * Inicializa la conexión con la base de datos
         * y lanza la aplicación grica.
         */
        LibraryInitializer.init();
        Application.launch(MainApp.class, args);

    }
}