package com.etl.pyapps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class pyDownloader {

    public void process_data() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "python",
                    "src/main/resources/Scripts/outlook_downloader.py"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Leer la salida del script
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            // si es un código 0 el proceso se ejecutó perfectamente

        } catch (IOException | InterruptedException e) {
            // gestionar las excepciones
        }
    }
}