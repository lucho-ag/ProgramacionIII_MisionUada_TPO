import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

// Punto de entrada: java Main [archivoMapa] [--log]
public class Main {
    /*
     * Ruta que se utiliza al ejecutar Main sin argumentos.
     * Es relativa al directorio de trabajo del proyecto.
     * Por ejemplo, coloca tus archivos en:
     *   d:\Documentos\Progra3_misionUada\pruebas\
     */
    private static final String ARCHIVO_PRUEBA = "pruebas\\mapa_grande.txt";

    public static void main(String[] args) throws Exception {
        String archivo = ARCHIVO_PRUEBA;
        MisionUada.logDetallado = false;
        for (String argumento : args) {
            if (argumento.equalsIgnoreCase("--log")) {
                MisionUada.logDetallado = true;
            } else {
                archivo = argumento;
            }
        }
        if (MisionUada.logDetallado) {
            MisionUada.archivoLog = new PrintWriter("log.txt", StandardCharsets.UTF_8);
        }
        try {
            MisionUada.misionUada(archivo);
        } finally {
            if (MisionUada.archivoLog != null) {
                MisionUada.archivoLog.close();
            }
        }
    }
}
