import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// 4.1 Clase Mapa
public class Mapa {
    List<Lugar> lugares = new ArrayList<>();
    List<Arista> aristas = new ArrayList<>();
    Map<Lugar, List<Arista>> salientes = new HashMap<>();   // listas de adyacencia (3.1.1)
    Lugar inicio;                                           // Aula 348
    Set<Lugar> obligatorios = new LinkedHashSet<>();
}
