// Chequeo manual (prueba 3): hijos de la raíz y dmin(Auditorio, Starbucks).
// Usa las funciones de MisionUada sin modificarlas. Uso: java Verificacion [archivoMapa]
public class Verificacion {
    private static final String ARCHIVO_PRUEBA = "pruebas\\prueba1.txt";

    public static void main(String[] args) throws Exception {
        String archivo = args.length > 0 ? args[0] : ARCHIVO_PRUEBA;
        MisionUada.mapa = MisionUada.leerMapa(archivo);
        MisionUada.prepararMapa(MisionUada.mapa);
        MisionUada.contadorId = 0;
        Nodo raiz = MisionUada.crearNodoRaiz(MisionUada.mapa);
        System.out.println("Raíz: CI=" + MisionUada.fmt(raiz.cotaInferior) + " CS=" + MisionUada.fmt(raiz.cotaSuperior));
        for (Candidato c : MisionUada.generarHijos(raiz)) {
            if (c.hijo != null) {
                System.out.println("  " + c.arista.destino + " [" + c.modo + "] t=" + MisionUada.fmt(c.hijo.tiempo)
                        + " bat=" + MisionUada.fmt(c.hijo.bateria) + " CI=" + MisionUada.fmt(c.hijo.cotaInferior)
                        + " motivo=" + c.motivo);
            } else {
                System.out.println("  " + c.arista.destino + " [" + c.modo + "] motivo=" + c.motivo);
            }
        }
        Lugar aud = MisionUada.buscarLugar(MisionUada.mapa, "Auditorio");
        Lugar star = MisionUada.buscarLugar(MisionUada.mapa, "Starbucks");
        System.out.println("dmin(Auditorio, Starbucks) = " + MisionUada.fmt(MisionUada.dmin[aud.indice][star.indice]));
        System.out.println("dmin(Starbucks, Auditorio) = " + MisionUada.fmt(MisionUada.dmin[star.indice][aud.indice]));
        for (String origen : new String[] {"Pasillo P3", "Aula 501"}) {
            Lugar v = MisionUada.buscarLugar(MisionUada.mapa, origen);
            for (Lugar o : MisionUada.mapa.obligatorios) {
                System.out.println("dmin(" + v + ", " + o + ") = " + MisionUada.fmt(MisionUada.dmin[v.indice][o.indice]));
            }
        }
    }
}
