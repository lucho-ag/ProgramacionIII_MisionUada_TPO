// 4.1 Clase Paso
public class Paso {
    Lugar origen, destino;
    Modo modo;
    double espera;
    double bateriaRestante;
    double tiempoAcumulado;

    Paso(Lugar origen, Lugar destino, Modo modo, double espera, double bateriaRestante, double tiempoAcumulado) {
        this.origen = origen;
        this.destino = destino;
        this.modo = modo;
        this.espera = espera;
        this.bateriaRestante = bateriaRestante;
        this.tiempoAcumulado = tiempoAcumulado;
    }
}
