package com.alphaone.logisticaRobots.domain.pathfinding;

import com.alphaone.logisticaRobots.domain.*;
import com.alphaone.logisticaRobots.shared.ParametrosGenerales;

import java.util.List;
import java.util.Set;

public class Planificador { //lo está haciendo tomi
    private Grafo grafo; //tiene un mapa
    private List<Pedido> pedidos; //tiene una lista de pedidos que priorizar y laburar
    private Set<Robopuerto> robopuertos;
    private Set<CofreLogistico> cofres;
    private Set<RobotLogistico> robotsLogisticos;
    private final GrillaEspacial grillaEspacial;

    public static final double factorConsumo = ParametrosGenerales.FACTOR_CONSUMO;

    public Planificador(Set<Robopuerto> robopuertos, GrillaEspacial grillaEspacial, Set<CofreLogistico> cofres, Set<RobotLogistico> robotsLogisticos, List<Pedido> pedidos) {
        this.robopuertos = robopuertos;
        this.grillaEspacial = grillaEspacial;
        this.cofres = cofres;
        this.robotsLogisticos = robotsLogisticos;
        this.pedidos = pedidos;
        construirGrafo();
    }


    private void construirGrafo() {
        this.grafo = new Grafo();

        // Agregar robopuertos como nodos
        for (Robopuerto robopuerto : robopuertos) {
            grafo.agregarNodo(new Nodo(robopuerto));
        }

        // Agregar cofres como nodos
        for (CofreLogistico cofre : cofres) {
            grafo.agregarNodo(new Nodo(cofre.getPosicion()));
        }

        // Conectar nodos basado en la cobertura y accesibilidad
        for (Nodo origen : grafo.getNodos()) {
            for (Nodo destino : grafo.getNodos()) {
                if (origen != destino) {
                    // Verificar si están en alcance
                    if (estanConectados(origen.getNodo(), destino.getNodo())) {
                        double distancia = origen.getNodo().distanciaHacia(destino.getNodo());
                        double peso = distancia * factorConsumo;
                        origen.agregarArista(new Arista(origen, destino, peso));
                    }
                }
            }
        }
    }

//    public Map<Nodo, Ruta> calcularRutasOptimas(Nodo origen) {
//        Map<Nodo, Double> distancias = new HashMap<>();
//        Map<Nodo, Nodo> predecesores = new HashMap<>();
//        Set<Nodo> nodosNoVisitados = new HashSet<>();
//
//        // Inicialización
//        for (Nodo nodo : grafo.getNodos()) {
//            distancias.put(nodo, Double.MAX_VALUE);
//            predecesores.put(nodo, null);
//            nodosNoVisitados.add(nodo);
//        }
//        distancias.put(origen, 0.0);
//
//        while (!nodosNoVisitados.isEmpty()) {
//            // Encontrar el nodo no visitado con la menor distancia
//            Nodo actual = null;
//            double menorDistancia = Double.MAX_VALUE;
//            for (Nodo nodo : nodosNoVisitados) {
//                if (distancias.get(nodo) < menorDistancia) {
//                    menorDistancia = distancias.get(nodo);
//                    actual = nodo;
//                }
//            }
//
//            if (actual == null || menorDistancia == Double.MAX_VALUE) {
//                break; // No hay más nodos alcanzables
//            }
//
//            nodosNoVisitados.remove(actual);
//
//            // Actualizar distancias de los vecinos
//            for (Arista arista : actual.getAristas()) {
//                Nodo vecino = arista.getDestino();
//                double distanciaAlternativa = distancias.get(actual) + arista.getPeso();
//
//                if (distanciaAlternativa < distancias.get(vecino)) {
//                    distancias.put(vecino, distanciaAlternativa);
//                    predecesores.put(vecino, actual);
//                }
//            }
//        }
//
//        // Construir las rutas óptimas
//        Map<Nodo, Ruta> rutasOptimas = new HashMap<>();
////        for (Nodo destino : grafo.getNodos()) {
////            if (predecesores.get(destino) != null || destino.equals(origen)) {
////                Ruta ruta = construirRuta(origen, destino, predecesores, distancias);
////                rutasOptimas.put(destino, ruta);
////            }
////        }
//
//        return rutasOptimas;
//    }

    //private Ruta construirRuta(Nodo origen, Nodo destino, Map<Nodo, Nodo> predecesores, Map<Nodo, Double> distancias) {

}
