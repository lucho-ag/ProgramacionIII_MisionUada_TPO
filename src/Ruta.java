import java.util.ArrayList;
import java.util.List;

// 4.1 Clase Ruta
public class Ruta {
    List<Lugar> secuencia = new ArrayList<>();       // identifica a la ruta (3.5.2)
    List<Paso> pasos = new ArrayList<>();
    double tiempoTotal;
    double bateriaFinal;
}
