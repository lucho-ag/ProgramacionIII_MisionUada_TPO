import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Genera mapas de prueba de distintos tamaños (sección 8 del informe), en el mismo
// formato que los mapas de la cátedra. No modifica el algoritmo.
//
// Cómo se arma cada mapa (siempre igual para la misma semilla):
//  - n lugares: el Aula 348 en (0, 0) y n - 1 lugares más, con coordenadas al azar.
//    Más o menos 4 de cada 10 son aulas (recargan batería).
//  - k obligatorios y una zona prohibida (si n >= 8).
//  - Un recorrido base que sale del 348, pasa por todos los lugares no prohibidos
//    en orden aleatorio y vuelve al 348, y dos variantes que intercambian dos lugares
//    consecutivos del recorrido: garantizan que existan al menos tres rutas válidas.
//  - Aristas dirigidas extra al azar, hasta un grado de salida promedio parecido al
//    del mapa grande (entre 1 y 3 aristas por lugar). Entre cada par de lugares hay
//    a lo sumo una conexión en cada sentido.
//  - Tres ventanas de tiempo en lugares al azar.
public class GeneradorMapas {

    private static final int[] TIEMPOS = {25, 30, 40, 50, 55, 60, 70};
    private static final String[] MODOS = {"CSP", "CSP", "CS", "CP"};

    public static void generar(int n, int k, long semilla, String archivo) throws IOException {
        Random r = new Random(semilla);
        List<String> nombres = new ArrayList<>();
        List<Integer> aulas = new ArrayList<>();
        nombres.add("Aula 348");
        aulas.add(348);
        Set<Integer> aulasUsadas = new HashSet<>();
        aulasUsadas.add(348);
        for (int i = 1; i < n; i++) {
            if (r.nextDouble() < 0.4) {
                int aula;
                do {
                    aula = 100 + r.nextInt(700);
                } while (aulasUsadas.contains(aula));
                aulasUsadas.add(aula);
                nombres.add("Aula " + aula);
                aulas.add(aula);
            } else {
                nombres.add("Lugar " + i);
                aulas.add(0);
            }
        }

        // obligatorios y zona prohibida
        List<Integer> otros = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            otros.add(i);
        }
        Collections.shuffle(otros, r);
        Set<Integer> obligatorios = new HashSet<>(otros.subList(0, Math.min(k, otros.size())));
        int prohibido = (n >= 8 && otros.size() > k) ? otros.get(k) : -1;

        // conexiones
        boolean[][] conectados = new boolean[n][n];
        List<String> aristas = new ArrayList<>();
        int[] grado = new int[n];

        List<Integer> recorrido = new ArrayList<>();
        for (int i = 1; i < n; i++) {
            if (i != prohibido) {
                recorrido.add(i);
            }
        }
        Collections.shuffle(recorrido, r);
        recorrido.add(0, 0);
        recorrido.add(0);
        agregarRecorrido(recorrido, r, conectados, aristas, nombres, grado);
        for (int variante = 0; variante < 2 && recorrido.size() > 4; variante++) {
            List<Integer> otro = new ArrayList<>(recorrido);
            int i = 1 + r.nextInt(otro.size() - 3);                    // intercambia dos lugares consecutivos
            Collections.swap(otro, i, i + 1);
            agregarRecorrido(otro, r, conectados, aristas, nombres, grado);
        }
        for (int u = 0; u < n; u++) {
            int objetivo = 1 + r.nextInt(3);                           // 1 a 3 aristas salientes
            int intentos = 0;
            while (grado[u] < objetivo && intentos < 50) {
                int v = r.nextInt(n);
                if (v != u && !conectados[u][v]) {
                    agregarArista(u, v, r, conectados, aristas, nombres, grado);
                }
                intentos++;
            }
        }

        try (PrintWriter out = new PrintWriter(archivo, StandardCharsets.UTF_8)) {
            out.printf("# Mapa generado: %d lugares, %d obligatorios, semilla %d%n", n, k, semilla);
            out.println("# LUGAR;nombre;aula(0 si no);x;y;obligatorio;prohibido");
            for (int i = 0; i < n; i++) {
                int x = i == 0 ? 0 : r.nextInt(201) - 100;
                int y = i == 0 ? 0 : r.nextInt(201) - 100;
                out.printf("LUGAR;%s;%d;%d;%d;%d;%d%n", nombres.get(i), aulas.get(i), x, y,
                        obligatorios.contains(i) ? 1 : 0, i == prohibido ? 1 : 0);
            }
            out.println("# ARISTA;origen;destino;segundos;modos");
            for (String a : aristas) {
                out.println(a);
            }
            out.println("# VENTANA;lugar;desde;hasta");
            for (int i = 0; i < 3 && n > 1; i++) {
                int lugar = 1 + r.nextInt(n - 1);
                int desde = r.nextInt(Math.max(1, 15 * n));
                out.printf("VENTANA;%s;%d;%d%n", nombres.get(lugar), desde, desde + 30 + r.nextInt(31));
            }
            out.println("INICIO;Aula 348");
        }
    }

    // Mapa de peor caso (sección 5.6): todos los lugares conectados con todos en ambos sentidos,
    // todas las aristas tardan lo mismo y permiten un único modo (caminando), todos los lugares
    // son obligatorios, sin zonas prohibidas ni ventanas. Todas las rutas tardan exactamente lo
    // mismo, así que ninguna cota inferior supera a la Cota y se recorre el árbol entero.
    public static void generarPeorCaso(int n, String archivo) throws IOException {
        try (PrintWriter out = new PrintWriter(archivo, StandardCharsets.UTF_8)) {
            out.printf("# Mapa de peor caso: %d lugares, todos obligatorios, aristas iguales%n", n);
            out.println("LUGAR;Aula 348;348;0;0;0;0");
            for (int i = 1; i < n; i++) {
                out.printf("LUGAR;Lugar %d;0;%d;%d;1;0%n", i, 10 * i, 5 * i);
            }
            for (int u = 0; u < n; u++) {
                for (int v = 0; v < n; v++) {
                    if (u != v) {
                        out.printf("ARISTA;%s;%s;30;C%n", u == 0 ? "Aula 348" : "Lugar " + u,
                                v == 0 ? "Aula 348" : "Lugar " + v);
                    }
                }
            }
            out.println("INICIO;Aula 348");
        }
    }

    private static void agregarRecorrido(List<Integer> recorrido, Random r, boolean[][] conectados,
                                         List<String> aristas, List<String> nombres, int[] grado) {
        for (int i = 0; i + 1 < recorrido.size(); i++) {
            agregarArista(recorrido.get(i), recorrido.get(i + 1), r, conectados, aristas, nombres, grado);
        }
    }

    private static void agregarArista(int u, int v, Random r, boolean[][] conectados, List<String> aristas,
                                      List<String> nombres, int[] grado) {
        if (conectados[u][v]) {
            return;                                                    // ya existe esa conexión en ese sentido
        }
        conectados[u][v] = true;
        aristas.add(String.format("ARISTA;%s;%s;%d;%s", nombres.get(u), nombres.get(v),
                TIEMPOS[r.nextInt(TIEMPOS.length)], MODOS[r.nextInt(MODOS.length)]));
        grado[u]++;
    }
}
