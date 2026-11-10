import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

// Medición de tiempos (sección 8 del informe). No modifica el algoritmo:
// llama a misionUada N veces con la salida silenciada y sin log detallado.
// Uso: java Medicion [archivoMapa] [N]
public class Medicion {
    private static final String ARCHIVO_PRUEBA = "pruebas\\prueba1.txt";

    public static void main(String[] args) throws Exception {
        String archivo = args.length > 0 ? args[0] : ARCHIVO_PRUEBA;
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        PrintStream consola = System.out;
        PrintStream nula = new PrintStream(OutputStream.nullOutputStream());
        MisionUada.logDetallado = false;

        double sumaMs = 0;
        int expandidos = 0, creados = 0;
        for (int corrida = 0; corrida <= n; corrida++) {           // la corrida 0 es de calentamiento
            MisionUada.reiniciarGlobales();
            System.setOut(nula);
            long inicio = System.nanoTime();
            MisionUada.misionUada(archivo);
            long fin = System.nanoTime();
            System.setOut(consola);
            if (corrida > 0) {
                sumaMs += (fin - inicio) / 1_000_000.0;
            }
            expandidos = MisionUada.contador.getOrDefault(Evento.EXPANDE, 0);
            creados = MisionUada.contadorId;
        }
        System.out.printf(Locale.forLanguageTag("es-AR"),
                "Mapa: %s%nCorridas medidas: %d (se descartó 1 de calentamiento)%nTiempo promedio: %.3f ms%n"
                        + "Nodos expandidos (EXPANDE): %d%nNodos creados (ids): %d%n",
                archivo, n, sumaMs / n, expandidos, creados);
    }
}
