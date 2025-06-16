package com.alphaone.logisticaRobots.domain.pathfinding;

import com.alphaone.logisticaRobots.domain.*;
import com.alphaone.logisticaRobots.shared.ParametrosGenerales;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

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
            grafo.agregarNodo(new Nodo(robopuerto.getPosicion()));
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

    /**
     * Determina si dos puntos están conectados en la red logística.
     * Dos puntos están conectados si están dentro de la grilla espacial
     * y la distancia entre ellos es razonable para que un robot pueda recorrerla.
     *
     * @param origen Punto de origen
     * @param destino Punto de destino
     * @return true si los puntos están conectados, false en caso contrario
     */
    private boolean estanConectados(Punto origen, Punto destino) {
        // Verificar que ambos puntos estén dentro de la grilla espacial
        if (!grillaEspacial.dentroDeGrilla(origen) || !grillaEspacial.dentroDeGrilla(destino)) {
            return false;
        }

        // Calcular la distancia entre los puntos
        double distancia = origen.distanciaHacia(destino);

        // Determinar si la distancia es razonable para un robot
        // Esto podría depender de la capacidad de batería de los robots
        // Por ahora, usamos un valor arbitrario como límite
        double distanciaMaxima = 50.0; // Valor arbitrario

        return distancia <= distanciaMaxima;
    }

    // Contador para generar IDs únicos para las rutas
    private static final AtomicInteger rutaIdCounter = new AtomicInteger(1);

    /**
     * Calcula las rutas óptimas desde un nodo de origen a todos los demás nodos
     * utilizando el algoritmo de Dijkstra.
     *
     * @param origen Nodo de origen
     * @return Mapa con las rutas óptimas a cada nodo
     */
    public Map<Nodo, Ruta> calcularRutasOptimas(Nodo origen) {
        Map<Nodo, Double> distancias = new HashMap<>();
        Map<Nodo, Nodo> predecesores = new HashMap<>();
        Set<Nodo> nodosNoVisitados = new HashSet<>();

        // Inicialización
        for (Nodo nodo : grafo.getNodos()) {
            distancias.put(nodo, Double.MAX_VALUE);
            predecesores.put(nodo, null);
            nodosNoVisitados.add(nodo);
        }
        distancias.put(origen, 0.0);

        while (!nodosNoVisitados.isEmpty()) {
            // Encontrar el nodo no visitado con la menor distancia
            Nodo actual = null;
            double menorDistancia = Double.MAX_VALUE;
            for (Nodo nodo : nodosNoVisitados) {
                if (distancias.get(nodo) < menorDistancia) {
                    menorDistancia = distancias.get(nodo);
                    actual = nodo;
                }
            }

            if (actual == null || menorDistancia == Double.MAX_VALUE) {
                break; // No hay más nodos alcanzables
            }

            nodosNoVisitados.remove(actual);

            // Actualizar distancias de los vecinos
            for (Arista arista : actual.getAristas()) {
                Nodo vecino = arista.getDestino();
                double distanciaAlternativa = distancias.get(actual) + arista.getPeso();

                if (distanciaAlternativa < distancias.get(vecino)) {
                    distancias.put(vecino, distanciaAlternativa);
                    predecesores.put(vecino, actual);
                }
            }
        }

        // Construir las rutas óptimas
        Map<Nodo, Ruta> rutasOptimas = new HashMap<>();
        for (Nodo destino : grafo.getNodos()) {
            if (predecesores.get(destino) != null || destino.equals(origen)) {
                Ruta ruta = construirRuta(origen, destino, predecesores);
                rutasOptimas.put(destino, ruta);
            }
        }

        return rutasOptimas;
    }

    /**
     * Construye una ruta desde un nodo de origen a un nodo de destino
     * utilizando los predecesores calculados por el algoritmo de Dijkstra.
     *
     * @param origen Nodo de origen
     * @param destino Nodo de destino
     * @param predecesores Mapa de predecesores
     * @return Ruta óptima desde el origen al destino
     */
    private Ruta construirRuta(Nodo origen, Nodo destino, Map<Nodo, Nodo> predecesores) {
        // Generar un ID único para la ruta
        int rutaId = rutaIdCounter.getAndIncrement();

        // Si el origen y el destino son el mismo, crear una ruta simple
        if (origen.equals(destino)) {
            return new Ruta(rutaId, origen.getNodo(), origen.getNodo());
        }

        // Si no hay predecesor para el destino, no hay ruta
        if (predecesores.get(destino) == null) {
            return null;
        }

        // Crear una ruta desde el origen al destino
        return new Ruta(rutaId, origen.getNodo(), destino.getNodo());
    }

}
