import java.util.ArrayList;
import java.util.List;

// 4.1 Clase Lugar
public class Lugar {
    String nombre;
    int aula;                                        // 0 si no es aula
    double x, y;                                     // coordenadas
    boolean obligatorio;
    boolean prohibido;
    List<Intervalo> ventanas = new ArrayList<>();    // intervalos [desde, hasta] en los que está cerrado

    int indice;                                      // ADICIÓN TÉCNICA: posición en mapa.lugares (fila/columna de dmin)

    Lugar(String nombre, int aula, double x, double y, boolean obligatorio, boolean prohibido) {
        this.nombre = nombre;
        this.aula = aula;
        this.x = x;
        this.y = y;
        this.obligatorio = obligatorio;
        this.prohibido = prohibido;
    }

    @Override
    public String toString() {                       // ADICIÓN TÉCNICA: para imprimir
        return nombre;
    }
}
