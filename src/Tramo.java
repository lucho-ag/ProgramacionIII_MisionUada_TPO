// 4.1 Clase Tramo
public class Tramo {
    Lugar origen, destino;
    Modo modo;
    double espera;                                   // segundos de espera al llegar (0 si el destino estaba abierto)

    Tramo(Lugar origen, Lugar destino, Modo modo, double espera) {
        this.origen = origen;
        this.destino = destino;
        this.modo = modo;
        this.espera = espera;
    }
}
