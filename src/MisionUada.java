import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * TPO "La misión de Uada" — Ramificación y Poda.
 * Traducción 1 a 1 del pseudocódigo del informe (sección 4).
 * Las ADICIONES TÉCNICAS (no presentes en el pseudocódigo) están marcadas y listadas en el README.
 */
public class MisionUada {

    // ═══════════════════════════════════════════════════════════════
    // 4.1 CONSTANTES Y VARIABLES GLOBALES
    // ═══════════════════════════════════════════════════════════════

    // Constantes
    static final EnumMap<Modo, Double> FACTOR_TIEMPO = new EnumMap<>(Map.of(Modo.C, 1.0, Modo.S, 0.5, Modo.P, 2.0));
    static final EnumMap<Modo, Double> FACTOR_CONSUMO = new EnumMap<>(Map.of(Modo.C, 1.0, Modo.S, 2.2, Modo.P, 0.45));
    static final double BATERIA_INICIAL = 100;
    static final double BATERIA_MAXIMA = 100;
    static final int CANTIDAD_RUTAS = 3;

    static final double INFINITO = Double.POSITIVE_INFINITY;           // ∞

    // Variables globales
    static Mapa mapa;
    static double[][] dmin;                                            // tiempos mínimos entre lugares (3.4.2), indexada por Lugar.indice
    static Map<Lugar, Double> entradaMax = new HashMap<>();            // entrada más costosa de cada lugar (3.4.4)
    static Map<Lugar, Double> durVentanas = new HashMap<>();           // duración total de las ventanas de cada lugar (3.4.4)
    static Map<Lugar, Double> distancia = new HashMap<>();             // distancia euclídea al Aula 348 (3.6.2)
    static double sumaSuperiorInicial;                                 // entradas y ventanas de todos los lugares salvo el inicio
    static double cota;                                                // Cota global (3.4.4)
    static List<Ruta> mejores = new ArrayList<>();                     // a lo sumo CANTIDAD_RUTAS rutas, ordenadas (3.5.3)
    static Nodo mejorTruncada;                                         // mejor ruta truncada registrada (3.6)
    static int contadorId;

    // Globales que usa registrarEnLog (7)
    static EnumMap<Evento, Integer> contador = new EnumMap<>(Evento.class);
    static EnumMap<Motivo, Integer> contadorMotivo = new EnumMap<>(Motivo.class);
    static boolean logDetallado = false;

    // ADICIONES TÉCNICAS (salida)
    static PrintWriter archivoLog;                                     // log.txt (lo abre/cierra Main)
    static Nodo ultimoExpandido;                                       // solo para imprimir el padre en las líneas DESCARTE
    static final Locale LOCALE = Locale.forLanguageTag("es-AR");       // números con coma decimal

    // ═══════════════════════════════════════════════════════════════
    // 4.2 FUNCIÓN PRINCIPAL
    // ═══════════════════════════════════════════════════════════════

    // 4.2 misionUada
    static void misionUada(String archivo) throws IOException {
        mapa = leerMapa(archivo);
        prepararMapa(mapa);                                            // precálculos (4.4.1)
        mejores = ramificacionYPoda(mapa);                             // búsqueda (4.3)
        if (!mejores.isEmpty()) {
            for (Ruta ruta : mejores) {
                mostrarRuta(ruta);
            }
        } else {
            if (mejorTruncada != null) {                               // objetivo de contingencia (3.6)
                mostrarRutaTruncada(reconstruirRuta(mejorTruncada));
            } else {
                System.out.println("Uada no puede moverse desde el Aula 348");
            }
        }
        mostrarResumenLog();                                           // contadores del log (7)
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.3 RAMIFICACIÓN Y PODA
    // ═══════════════════════════════════════════════════════════════

    // 4.3 ramificacionYPoda
    static List<Ruta> ramificacionYPoda(Mapa mapa) {
        PriorityQueue<Nodo> nodosVivos = inicColaPrioridad();          // menor cota inferior, mayor nivel, menor id (3.4.7)
        mejores = new ArrayList<>();
        mejorTruncada = null;
        contadorId = 0;
        Nodo raiz = crearNodoRaiz(mapa);
        cota = raiz.cotaSuperior;                                      // Cota global inicial (3.4.4)
        nodosVivos.add(raiz);
        boolean continuar = true;
        while (!nodosVivos.isEmpty() && continuar) {
            Nodo nodo = nodosVivos.poll();                             // el más prometedor
            if (nodo != raiz && poda(nodo, cota)) {                    // la Cota pudo bajar mientras esperaba
                registrarEnLog(Evento.FIN, nodo);
                continuar = false;                                     // los nodos restantes también se podarían (3.5.5)
            } else {
                registrarEnLog(Evento.EXPANDE, nodo);
                for (Candidato c : generarHijos(nodo)) {               // RAMIFICACIÓN
                    if (c.motivo != Motivo.OK) {                       // poda por factibilidad
                        registrarEnLog(Evento.DESCARTE, c);
                        if (mejores.isEmpty()) {
                            if (c.motivo == Motivo.SIN_SALIDA) {
                                registrarTruncada(c.hijo);             // Uada llega al destino
                            } else {
                                registrarTruncada(nodo);               // Uada queda en el lugar actual
                            }
                        }
                    } else {
                        if (esSolucion(c.hijo)) {
                            registrarEnLog(Evento.SOLUCION, c.hijo);
                            registrarSolucion(c.hijo);                 // puede actualizar la Cota
                        } else {
                            if (poda(c.hijo, cota)) {                  // PODA por cota
                                registrarEnLog(Evento.PODA_COTA, c.hijo);
                                if (mejores.isEmpty()) {
                                    registrarTruncada(c.hijo);
                                }
                            } else {
                                nodosVivos.add(c.hijo);
                                registrarEnLog(Evento.VIVO, c.hijo);
                            }
                        }
                    }
                }
            }
        }
        return mejores;
    }

    // 4.3 inicColaPrioridad: menor cotaInferior, luego mayor nivel, luego menor id
    static PriorityQueue<Nodo> inicColaPrioridad() {
        return new PriorityQueue<>(Comparator
                .comparingDouble((Nodo n) -> n.cotaInferior)
                .thenComparing((Nodo n) -> n.nivel, Comparator.reverseOrder())
                .thenComparingInt((Nodo n) -> n.id));
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.1 LECTURA Y PREPARACIÓN DEL MAPA
    // ═══════════════════════════════════════════════════════════════

    // 4.4.1 leerMapa  (campos[1..7] del pseudocódigo = campos[0..6] en Java)
    static Mapa leerMapa(String archivo) throws IOException {
        Mapa mapa = new Mapa();
        for (String linea : Files.readAllLines(Paths.get(archivo), StandardCharsets.UTF_8)) {
            linea = linea.replace("﻿", "").trim();               // ADICIÓN TÉCNICA: quita BOM y espacios
            if (!linea.startsWith("#") && !linea.isEmpty()) {         // las líneas con # son comentarios (ADICIÓN: se saltean vacías)
                String[] campos = linea.split(";");
                for (int i = 0; i < campos.length; i++) {
                    campos[i] = campos[i].trim();
                }
                if (campos[0].equals("LUGAR")) {
                    Lugar v = new Lugar(campos[1], Integer.parseInt(campos[2]),
                            Double.parseDouble(campos[3]), Double.parseDouble(campos[4]),
                            campos[5].equals("1"), campos[6].equals("1"));
                    v.indice = mapa.lugares.size();                    // ADICIÓN TÉCNICA: índice para dmin
                    mapa.lugares.add(v);
                    mapa.salientes.put(v, new ArrayList<>());
                    if (v.obligatorio) {
                        mapa.obligatorios.add(v);
                    }
                } else if (campos[0].equals("ARISTA")) {
                    Arista a = new Arista(buscarLugar(mapa, campos[1]), buscarLugar(mapa, campos[2]),
                            Double.parseDouble(campos[3]), parsearModos(campos[4]));
                    mapa.aristas.add(a);
                    mapa.salientes.get(a.origen).add(a);
                } else if (campos[0].equals("VENTANA")) {
                    buscarLugar(mapa, campos[1]).ventanas.add(
                            new Intervalo(Double.parseDouble(campos[2]), Double.parseDouble(campos[3])));
                } else if (campos[0].equals("INICIO")) {
                    mapa.inicio = buscarLugar(mapa, campos[1]);
                }
            }
        }
        return mapa;
    }

    // 4.4.1 buscarLugar
    static Lugar buscarLugar(Mapa mapa, String nombre) {
        for (Lugar v : mapa.lugares) {
            if (v.nombre.equals(nombre)) {
                return v;
            }
        }
        throw new IllegalArgumentException("Lugar inexistente en el mapa: " + nombre);
    }

    // ADICIÓN TÉCNICA: el campo modos viene con las letras juntas, por ejemplo "CSP", "CS" o "CP"
    static Set<Modo> parsearModos(String texto) {
        Set<Modo> modos = EnumSet.noneOf(Modo.class);
        for (char letra : texto.toCharArray()) {
            modos.add(Modo.valueOf(String.valueOf(letra)));
        }
        return modos;
    }

    // 4.4.1 prepararMapa
    static void prepararMapa(Mapa mapa) {
        dmin = floyd(mapa);
        for (Lugar v : mapa.lugares) {
            entradaMax.put(v, 0.0);
            durVentanas.put(v, 0.0);
            for (Intervalo ventana : v.ventanas) {
                durVentanas.put(v, durVentanas.get(v) + (ventana.hasta - ventana.desde));
            }
            distancia.put(v, Math.sqrt(Math.pow(v.x - mapa.inicio.x, 2) + Math.pow(v.y - mapa.inicio.y, 2)));
        }
        for (Arista a : mapa.aristas) {
            if (!a.origen.prohibido) {
                entradaMax.put(a.destino, Math.max(entradaMax.get(a.destino), tiempoMasLento(a)));
            }
        }
        sumaSuperiorInicial = 0;
        for (Lugar v : mapa.lugares) {
            if (!v.prohibido && v != mapa.inicio) {
                sumaSuperiorInicial = sumaSuperiorInicial + entradaMax.get(v) + durVentanas.get(v);
            }
        }
    }

    // 4.4.1 floyd
    static double[][] floyd(Mapa mapa) {
        int n = mapa.lugares.size();
        double[][] d = new double[n][n];
        for (Lugar u : mapa.lugares) {
            for (Lugar v : mapa.lugares) {
                if (u == v) {
                    d[u.indice][v.indice] = 0;
                } else {
                    d[u.indice][v.indice] = INFINITO;
                }
            }
        }
        for (Arista a : mapa.aristas) {                                // pesos del grafo relajado
            if (!a.origen.prohibido && !a.destino.prohibido) {
                d[a.origen.indice][a.destino.indice] = Math.min(d[a.origen.indice][a.destino.indice], tiempoMasRapido(a));
            }
        }
        for (Lugar k : mapa.lugares) {                                 // k: posible lugar intermedio
            if (!k.prohibido && k != mapa.inicio) {
                for (Lugar i : mapa.lugares) {
                    for (Lugar j : mapa.lugares) {
                        if (d[i.indice][k.indice] + d[k.indice][j.indice] < d[i.indice][j.indice]) {
                            d[i.indice][j.indice] = d[i.indice][k.indice] + d[k.indice][j.indice];
                        }
                    }
                }
            }
        }
        return d;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.2 MODOS DE DESPLAZAMIENTO, BATERÍA Y ESPERA
    // ═══════════════════════════════════════════════════════════════

    // 4.4.2 tiempoTramo
    static double tiempoTramo(Arista a, Modo m) {
        return FACTOR_TIEMPO.get(m) * a.tiempoBase;
    }

    // 4.4.2 consumoTramo
    static double consumoTramo(Arista a, Modo m) {
        return FACTOR_CONSUMO.get(m) * a.tiempoBase / 60;
    }

    // 4.4.2 tiempoMasRapido
    static double tiempoMasRapido(Arista a) {
        double menor = INFINITO;
        for (Modo m : a.modos) {
            menor = Math.min(menor, tiempoTramo(a, m));
        }
        return menor;
    }

    // 4.4.2 tiempoMasLento
    static double tiempoMasLento(Arista a) {
        double mayor = 0;
        for (Modo m : a.modos) {
            mayor = Math.max(mayor, tiempoTramo(a, m));
        }
        return mayor;
    }

    // 4.4.2 recarga
    static double recarga(Lugar v) {
        if (v.aula == 0 || v == mapa.inicio) {                         // el Aula 348 no recarga
            return 0;
        }
        return sumaDigitos(v.aula) / 5.0;
    }

    // 4.4.2 sumaDigitos
    static int sumaDigitos(int n) {
        int suma = 0;
        while (n > 0) {
            suma = suma + (n % 10);
            n = n / 10;
        }
        return suma;
    }

    // 4.4.2 aplicarEspera
    static double aplicarEspera(Lugar v, double llegada) {
        double t = llegada;
        boolean espero = true;
        while (espero) {
            espero = false;
            for (Intervalo ventana : v.ventanas) {
                if (ventana.desde <= t && t < ventana.hasta) {         // el lugar está cerrado
                    t = ventana.hasta;                                 // espera hasta que vuelve a abrir
                    espero = true;
                }
            }
        }
        return t;                                                      // la espera no consume batería
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.3 CREACIÓN DE NODOS
    // ═══════════════════════════════════════════════════════════════

    // 4.4.3 crearNodoRaiz
    static Nodo crearNodoRaiz(Mapa mapa) {
        Nodo raiz = new Nodo();
        raiz.lugar = mapa.inicio;
        raiz.tiempo = 0;
        raiz.bateria = BATERIA_INICIAL;
        raiz.visitados = new LinkedHashSet<>(Set.of(mapa.inicio));
        raiz.faltantes = new LinkedHashSet<>(mapa.obligatorios);       // copia
        raiz.padre = null;
        raiz.tramo = null;
        raiz.nivel = 0;
        raiz.id = nuevoId();
        raiz.restoSuperior = sumaSuperiorInicial;
        calcularCotas(raiz);
        return raiz;
    }

    // 4.4.3 crearHijo
    static Nodo crearHijo(Nodo padre, Arista a, Modo m, double tiempoSalida, double espera, double bateriaLlegada) {
        Lugar v = a.destino;
        Nodo hijo = new Nodo();
        hijo.lugar = v;
        hijo.tiempo = tiempoSalida;                                    // llegada más la espera
        hijo.bateria = Math.min(BATERIA_MAXIMA, bateriaLlegada + recarga(v));
        hijo.visitados = new LinkedHashSet<>(padre.visitados);         // copia ∪ {v}
        hijo.visitados.add(v);
        hijo.faltantes = new LinkedHashSet<>(padre.faltantes);         // copia \ {v}
        hijo.faltantes.remove(v);
        hijo.padre = padre;
        hijo.tramo = new Tramo(padre.lugar, v, m, espera);
        hijo.nivel = padre.nivel + 1;
        hijo.id = nuevoId();
        if (v != mapa.inicio) {
            hijo.restoSuperior = padre.restoSuperior - (entradaMax.get(v) + durVentanas.get(v));
        } else {
            hijo.restoSuperior = 0;                                    // solución: no falta recorrer nada
        }
        return hijo;
    }

    // 4.4.3 nuevoId
    static int nuevoId() {
        contadorId = contadorId + 1;
        return contadorId;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.4 GENERACIÓN DE HIJOS Y PODAS POR FACTIBILIDAD
    // ═══════════════════════════════════════════════════════════════

    // 4.4.4 generarHijos
    static List<Candidato> generarHijos(Nodo nodo) {
        List<Candidato> candidatos = new ArrayList<>();
        for (Arista a : mapa.salientes.get(nodo.lugar)) {
            for (Modo m : a.modos) {
                Candidato c = new Candidato(a, m);
                c.motivo = verificarDestino(nodo, a.destino);                  // condiciones 1 a 3
                if (c.motivo == Motivo.OK) {
                    double llegada = nodo.tiempo + tiempoTramo(a, m);
                    double tiempoSalida = aplicarEspera(a.destino, llegada);   // espera si está cerrado
                    double bateriaLlegada = nodo.bateria - consumoTramo(a, m); // antes de la recarga
                    c.motivo = verificarBateria(a.destino, bateriaLlegada);    // condición 4
                    if (c.motivo == Motivo.OK) {
                        c.hijo = crearHijo(nodo, a, m, tiempoSalida, tiempoSalida - llegada, bateriaLlegada);
                        if (tieneSalida(c.hijo)) {                             // condición 5
                            calcularCotas(c.hijo);
                        } else {
                            c.motivo = Motivo.SIN_SALIDA;
                        }
                    }
                }
                candidatos.add(c);
            }
        }
        return candidatos;
    }

    // 4.4.4 verificarDestino
    static Motivo verificarDestino(Nodo nodo, Lugar v) {
        if (v.prohibido) {
            return Motivo.PROHIBIDO;
        }
        if (nodo.visitados.contains(v) && v != mapa.inicio) {
            return Motivo.REPETIDO;
        }
        if (v == mapa.inicio && !nodo.faltantes.isEmpty()) {
            return Motivo.VUELTA_PREMATURA;
        }
        return Motivo.OK;
    }

    // 4.4.4 verificarBateria
    static Motivo verificarBateria(Lugar v, double bateriaLlegada) {
        if (v == mapa.inicio && bateriaLlegada < 0) {                  // llegar con 0 al Aula 348 es válido
            return Motivo.BATERIA;
        }
        if (v != mapa.inicio && bateriaLlegada <= 0) {
            return Motivo.BATERIA;
        }
        return Motivo.OK;
    }

    // 4.4.4 tieneSalida
    static boolean tieneSalida(Nodo hijo) {
        Lugar v = hijo.lugar;
        if (v == mapa.inicio) {                                        // es solución
            return true;
        }
        if (dmin[v.indice][mapa.inicio.indice] == INFINITO) {
            return false;
        }
        for (Lugar o : hijo.faltantes) {
            if (dmin[v.indice][o.indice] == INFINITO || dmin[o.indice][mapa.inicio.indice] == INFINITO) {
                return false;
            }
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.5 COTAS Y PODA POR COTA
    // ═══════════════════════════════════════════════════════════════

    // 4.4.5 calcularCotas
    static void calcularCotas(Nodo n) {
        if (n.lugar == mapa.inicio && n.nivel > 0) {                                       // solución
            n.cotaInferior = n.tiempo;
            n.cotaSuperior = n.tiempo;
        } else {
            n.cotaInferior = n.tiempo + estimacionRestante(n.lugar, n.faltantes);          // 3.4.3
            n.cotaSuperior = n.tiempo + entradaMax.get(mapa.inicio) + durVentanas.get(mapa.inicio)
                    + n.restoSuperior;                                                     // 3.4.4
        }
    }

    // 4.4.5 estimacionRestante
    static double estimacionRestante(Lugar v, Set<Lugar> faltantes) {
        if (faltantes.isEmpty()) {
            return dmin[v.indice][mapa.inicio.indice];
        }
        double entrada = INFINITO;
        double salida = INFINITO;
        for (Lugar o : faltantes) {
            entrada = Math.min(entrada, dmin[v.indice][o.indice]);             // obligatorio más cercano
            salida = Math.min(salida, dmin[o.indice][mapa.inicio.indice]);     // vuelta más corta al Aula 348
        }
        return entrada + mstObligatorios(faltantes) + salida;
    }

    // 4.4.5 mstObligatorios (Prim)
    static double mstObligatorios(Set<Lugar> faltantes) {
        if (faltantes.size() <= 1) {
            return 0;
        }
        Lugar primero = faltantes.iterator().next();                   // cualquier elemento de faltantes
        Set<Lugar> enArbol = new LinkedHashSet<>(Set.of(primero));
        Map<Lugar, Double> costo = new HashMap<>();
        for (Lugar o : faltantes) {
            if (!enArbol.contains(o)) {
                costo.put(o, peso(primero, o));                        // menor peso para unir o al árbol
            }
        }
        double total = 0;
        while (enArbol.size() < faltantes.size()) {
            Lugar o = null;                                            // elemento de faltantes \ enArbol con menor costo[o]
            for (Lugar p : faltantes) {
                if (!enArbol.contains(p) && (o == null || costo.get(p) < costo.get(o))) {
                    o = p;
                }
            }
            if (costo.get(o) == INFINITO) {
                return INFINITO;
            }
            total = total + costo.get(o);
            enArbol.add(o);
            for (Lugar p : faltantes) {
                if (!enArbol.contains(p)) {
                    costo.put(p, Math.min(costo.get(p), peso(o, p)));
                }
            }
        }
        return total;
    }

    // 4.4.5 peso
    static double peso(Lugar a, Lugar b) {
        return Math.min(dmin[a.indice][b.indice], dmin[b.indice][a.indice]);   // menor de los dos sentidos (3.4.3)
    }

    // 4.4.5 poda
    static boolean poda(Nodo n, double cota) {
        return n.cotaInferior > cota;                                  // desigualdad estricta (3.4.5)
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.6 SOLUCIONES Y COTA GLOBAL
    // ═══════════════════════════════════════════════════════════════

    // 4.4.6 esSolucion
    static boolean esSolucion(Nodo n) {
        return n.lugar == mapa.inicio && n.faltantes.isEmpty();
    }

    // 4.4.6 registrarSolucion
    static void registrarSolucion(Nodo n) {
        if (mejores.size() == CANTIDAD_RUTAS && n.tiempo > cota) {
            return;                                                    // no puede formar parte del resultado
        }
        Ruta ruta = reconstruirRuta(n);
        int i = posicion(mejores, ruta.secuencia);                     // -1 si la secuencia no está en la lista
        if (i >= 0) {                                                  // (pseudocódigo 1-based: i > 0) misma secuencia con otros modos
            if (esMejor(ruta, mejores.get(i))) {
                mejores.set(i, ruta);
            }
        } else {
            if (mejores.size() < CANTIDAD_RUTAS) {
                mejores.add(ruta);
            } else {
                if (esMejor(ruta, mejores.get(mejores.size() - 1))) {
                    mejores.set(mejores.size() - 1, ruta);             // la tercera sale de la lista (reemplazarUltimo)
                }
            }
        }
        ordenarEstable(mejores);
        if (mejores.size() == CANTIDAD_RUTAS) {
            cota = mejores.get(mejores.size() - 1).tiempoTotal;        // actualización de la Cota (3.5.3)
        }
    }

    // 4.4.6 posicion: el pseudocódigo es 1-based y devuelve 0 si no está;
    // en Java es 0-based y devuelve -1 si no está.
    static int posicion(List<Ruta> mejores, List<Lugar> secuencia) {
        for (int i = 0; i < mejores.size(); i++) {
            if (mejores.get(i).secuencia.equals(secuencia)) {
                return i;
            }
        }
        return -1;
    }

    // 4.4.6 ordenarEstable(mejores, esMejor): List.sort es estable en Java
    static void ordenarEstable(List<Ruta> mejores) {
        mejores.sort((r1, r2) -> esMejor(r1, r2) ? -1 : (esMejor(r2, r1) ? 1 : 0));
    }

    // 4.4.6 esMejor
    static boolean esMejor(Ruta r1, Ruta r2) {
        return r1.tiempoTotal < r2.tiempoTotal
                || (r1.tiempoTotal == r2.tiempoTotal && r1.bateriaFinal > r2.bateriaFinal);
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.7 OBJETIVO DE CONTINGENCIA
    // ═══════════════════════════════════════════════════════════════

    // 4.4.7 registrarTruncada
    static void registrarTruncada(Nodo n) {
        if (n.nivel == 0) {                                            // sin tramos no es una ruta (3.6.3)
            return;
        }
        if (mejorTruncada == null || truncadaMejor(n, mejorTruncada)) {
            mejorTruncada = n;
        }
    }

    // 4.4.7 truncadaMejor
    static boolean truncadaMejor(Nodo a, Nodo b) {
        if (!distancia.get(a.lugar).equals(distancia.get(b.lugar))) {
            return distancia.get(a.lugar) < distancia.get(b.lugar);
        }
        if (a.faltantes.size() != b.faltantes.size()) {
            return a.faltantes.size() < b.faltantes.size();
        }
        return a.tiempo < b.tiempo;
    }

    // ═══════════════════════════════════════════════════════════════
    // 4.4.8 RECONSTRUCCIÓN DE LA RUTA Y REGISTRO EN EL LOG
    // ═══════════════════════════════════════════════════════════════

    // 4.4.8 reconstruirRuta
    static Ruta reconstruirRuta(Nodo n) {
        Ruta ruta = new Ruta();
        Nodo actual = n;
        while (actual.padre != null) {
            Paso paso = new Paso(actual.tramo.origen, actual.tramo.destino, actual.tramo.modo,
                    actual.tramo.espera, actual.bateria, actual.tiempo);
            ruta.pasos.add(paso);
            ruta.secuencia.add(actual.lugar);
            actual = actual.padre;
        }
        ruta.secuencia.add(actual.lugar);                              // la raíz: Aula 348
        Collections.reverse(ruta.pasos);
        Collections.reverse(ruta.secuencia);
        ruta.tiempoTotal = n.tiempo;
        ruta.bateriaFinal = n.bateria;
        return ruta;
    }

    // 4.4.8 registrarEnLog
    static void registrarEnLog(Evento evento, Object elemento) {
        contador.merge(evento, 1, Integer::sum);                       // contador[evento] + 1
        if (evento == Evento.DESCARTE) {
            contadorMotivo.merge(((Candidato) elemento).motivo, 1, Integer::sum);
        }
        if (logDetallado) {
            escribirLinea(evento, elemento);
        }
    }

    // 4.4.8 escribirLinea: una línea por evento, indentada 2 espacios por nivel
    static void escribirLinea(Evento evento, Object elemento) {
        String linea;
        if (elemento instanceof Candidato) {                           // DESCARTE
            Candidato c = (Candidato) elemento;
            Nodo padre = ultimoExpandido;
            Nodo h = c.hijo;                                           // NULO salvo en SIN_SALIDA
            int nivel = padre.nivel + 1;
            linea = "  ".repeat(nivel) + String.format(LOCALE,
                    "%-9s id=%s padre=%d nivel=%d lugar=%s modo=%s t=%s bat=%s CI=- CS=- motivo=%s",
                    evento, h == null ? "-" : String.valueOf(h.id), padre.id, nivel, c.arista.destino, c.modo,
                    h == null ? "-" : fmt(h.tiempo), h == null ? "-" : fmt(h.bateria), c.motivo);
            // (en un descarte las cotas no se calculan: calcularCotas solo corre si tieneSalida)
        } else {
            Nodo n = (Nodo) elemento;
            if (evento == Evento.EXPANDE) {
                ultimoExpandido = n;
            }
            linea = "  ".repeat(n.nivel) + String.format(LOCALE,
                    "%-9s id=%d padre=%s nivel=%d lugar=%s modo=%s t=%s bat=%s CI=%s CS=%s",
                    evento, n.id, n.padre == null ? "-" : String.valueOf(n.padre.id), n.nivel, n.lugar,
                    n.tramo == null ? "-" : n.tramo.modo, fmt(n.tiempo), fmt(n.bateria),
                    fmt(n.cotaInferior), fmt(n.cotaSuperior));
        }
        System.out.println(linea);
        if (archivoLog != null) {
            archivoLog.println(linea);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SALIDAS: mostrarRuta, mostrarRutaTruncada, mostrarResumenLog
    // ═══════════════════════════════════════════════════════════════

    // mostrarRuta
    static void mostrarRuta(Ruta ruta) {
        System.out.println();
        System.out.println("Ruta " + (mejores.indexOf(ruta) + 1) + ": " + secuenciaComoTexto(ruta));
        imprimirTabla(ruta);
        System.out.println("Tiempo total: " + fmt(ruta.tiempoTotal) + " s   Batería final: " + fmt(ruta.bateriaFinal));
    }

    // mostrarRutaTruncada
    static void mostrarRutaTruncada(Ruta ruta) {
        System.out.println();
        System.out.println("No existe ninguna ruta válida. Ruta truncada (objetivo de contingencia):");
        System.out.println(secuenciaComoTexto(ruta));
        imprimirTabla(ruta);
        Lugar ultimo = ruta.secuencia.get(ruta.secuencia.size() - 1);
        int obligatoriosVisitados = 0;
        for (Lugar v : ruta.secuencia) {
            if (v.obligatorio) {
                obligatoriosVisitados++;
            }
        }
        System.out.println("Último lugar alcanzado: " + ultimo);
        System.out.println("Obligatorios visitados: " + obligatoriosVisitados + " de " + mapa.obligatorios.size());
        System.out.println("Distancia euclídea al " + mapa.inicio + ": " + fmt(distancia.get(ultimo)));
        System.out.println("Tiempo acumulado: " + fmt(ruta.tiempoTotal) + " s   Batería: " + fmt(ruta.bateriaFinal));
    }

    // mostrarResumenLog
    static void mostrarResumenLog() {
        System.out.println();
        System.out.println("── Resumen del log ──");
        System.out.println("Nodos creados (ids asignados): " + contadorId);
        for (Evento e : Evento.values()) {
            System.out.printf("  %-10s %d%n", e, contador.getOrDefault(e, 0));
        }
        System.out.println("Descartes por motivo:");
        for (Motivo m : Motivo.values()) {
            if (m != Motivo.OK) {
                System.out.printf("  %-17s %d%n", m, contadorMotivo.getOrDefault(m, 0));
            }
        }
    }

    // ── ADICIONES TÉCNICAS de formato ──

    static void imprimirTabla(Ruta ruta) {
        String formato = "%-5s | %-16s | %-16s | %-4s | %8s | %16s | %16s%n";
        System.out.printf(formato, "Paso", "Origen", "Destino", "Modo", "Espera", "Batería restante", "Tiempo acumulado");
        System.out.println("-".repeat(97));
        int i = 1;
        for (Paso p : ruta.pasos) {
            System.out.printf(formato, i++, p.origen, p.destino, p.modo, fmt(p.espera), fmt(p.bateriaRestante), fmt(p.tiempoAcumulado));
        }
    }

    static String secuenciaComoTexto(Ruta ruta) {
        StringBuilder sb = new StringBuilder();
        for (Lugar v : ruta.secuencia) {
            if (sb.length() > 0) {
                sb.append(" → ");
            }
            sb.append(v);
        }
        return sb.toString();
    }

    static String fmt(double valor) {
        return Double.isInfinite(valor) ? "∞" : String.format(LOCALE, "%.2f", valor);
    }

    // Reinicia los acumuladores entre corridas (lo usa Medicion; el pseudocódigo corre una sola vez)
    static void reiniciarGlobales() {
        entradaMax = new HashMap<>();
        durVentanas = new HashMap<>();
        distancia = new HashMap<>();
        contador = new EnumMap<>(Evento.class);
        contadorMotivo = new EnumMap<>(Motivo.class);
    }
}
