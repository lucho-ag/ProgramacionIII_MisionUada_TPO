// 4.1 Clase Candidato
public class Candidato {
    Arista arista;
    Modo modo;
    Motivo motivo;                                   // OK o el motivo de descarte (3.4.1)
    Nodo hijo;                                       // NULO si el destino no puede alcanzarse

    Candidato(Arista arista, Modo modo) {
        this.arista = arista;
        this.modo = modo;
    }
}
