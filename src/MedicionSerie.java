import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// Complejidad temporal práctica (sección 8 del informe). No modifica el algoritmo:
// igual que Medicion, llama a misionUada con la salida silenciada y sin log detallado,
// y mide con System.nanoTime() sobre mapas de distintos tamaños.
//
// Uso: java MedicionSerie lugares   serie con mapas al azar (k = 4), 10 mapas por tamaño
//      java MedicionSerie peor      serie con mapas de peor caso (sección 5.6)
//
// Cada mapa se ejecuta 3 veces sin medir (para que la JVM compile el código) y después
// se mide entre 3 y 20 veces (menos veces si cada corrida tarda mucho). Por tamaño se
// informa la mediana de los mapas, porque mapas del mismo tamaño varían mucho entre sí.
public class MedicionSerie {
    static final int SEMILLAS = 10;
    static final int CALENTAMIENTO = 3;
    static final Locale AR = Locale.forLanguageTag("es-AR");
    static PrintStream consola = System.out;
    static PrintStream nula = new PrintStream(OutputStream.nullOutputStream());

    public static void main(String[] args) throws Exception {
        boolean peor = args.length > 0 && args[0].equalsIgnoreCase("peor");
        new File("pruebas/generados").mkdirs();
        MisionUada.logDetallado = false;
        int[] tamanios = peor ? new int[]{4, 5, 6, 7, 8, 9, 10, 11} : new int[]{8, 12, 16, 20, 24, 28};
        consola.println("lugares;obligatorios;mapas;nodos creados (mediana);nodos expandidos (mediana);"
                + "tiempo ms (mediana);tiempo ms (mín);tiempo ms (máx)");
        for (int n : tamanios) {
            int semillas = peor ? 1 : SEMILLAS;
            int k = peor ? n - 1 : 4;
            List<Double> tiempos = new ArrayList<>();
            List<Long> creados = new ArrayList<>(), expandidos = new ArrayList<>();
            for (int s = 1; s <= semillas; s++) {
                String archivo = peor ? String.format("pruebas/generados/peor_n%d.txt", n)
                                      : String.format("pruebas/generados/mapa_n%d_s%d.txt", n, s);
                if (peor) {
                    GeneradorMapas.generarPeorCaso(n, archivo);
                } else {
                    GeneradorMapas.generar(n, k, 1000L * n + 10L * k + s, archivo);
                }
                tiempos.add(medir(archivo));
                creados.add((long) MisionUada.contadorId);
                expandidos.add((long) MisionUada.contador.getOrDefault(Evento.EXPANDE, 0));
                
            }
            consola.printf(AR, "%d;%d;%d;%d;%d;%.3f;%.3f;%.3f%n", n, k, semillas, mediana(creados), mediana(expandidos),
                    medianaD(tiempos), Collections.min(tiempos), Collections.max(tiempos));
        }
    }

    // Promedio de las corridas medidas de un mapa, en milisegundos
    static double medir(String archivo) throws Exception {
        for (int i = 0; i < CALENTAMIENTO; i++) {
            correr(archivo);
        }
        double suma = 0;
        int corridas = 0;
        while (corridas < 3 || (corridas < 20 && suma < 2000)) {
            suma += correr(archivo);
            corridas++;
        }
        return suma / corridas;
    }

    static double correr(String archivo) throws Exception {
        MisionUada.reiniciarGlobales();
        System.setOut(nula);
        long inicio = System.nanoTime();
        MisionUada.misionUada(archivo);
        long fin = System.nanoTime();
        System.setOut(consola);
        return (fin - inicio) / 1_000_000.0;
    }

    static long mediana(List<Long> v) {
        List<Long> c = new ArrayList<>(v);
        Collections.sort(c);
        return c.get(c.size() / 2);
    }

    static double medianaD(List<Double> v) {
        List<Double> c = new ArrayList<>(v);
        Collections.sort(c);
        return c.get(c.size() / 2);
    }
}
