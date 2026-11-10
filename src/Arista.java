import java.util.Set;

// 4.1 Clase Arista
public class Arista {
    Lugar origen, destino;                           // conexión dirigida: solo de origen hacia destino
    double tiempoBase;                               // segundos que tarda caminando
    Set<Modo> modos;                                 // subconjunto de {C, S, P}

    Arista(Lugar origen, Lugar destino, double tiempoBase, Set<Modo> modos) {
        this.origen = origen;
        this.destino = destino;
        this.tiempoBase = tiempoBase;
        this.modos = modos;
    }
}
