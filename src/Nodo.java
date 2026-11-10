import java.util.Set;

// 4.1 Clase Nodo
public class Nodo {
    Lugar lugar;                                     // último lugar alcanzado
    double tiempo;                                   // tiempo acumulado, incluidas las esperas
    double bateria;                                  // batería al llegar, con la recarga ya aplicada
    Set<Lugar> visitados;
    Set<Lugar> faltantes;                            // obligatorios todavía no visitados
    double cotaInferior;
    double cotaSuperior;
    double restoSuperior;                            // entradas y ventanas de los lugares no visitados (3.4.4)
    Nodo padre;
    Tramo tramo;                                     // tramo que une al padre con este nodo
    int nivel;
    int id;
}
