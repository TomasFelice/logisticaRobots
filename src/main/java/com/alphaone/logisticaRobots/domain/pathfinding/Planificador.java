package com.alphaone.logisticaRobots.domain.pathfinding;

import com.alphaone.logisticaRobots.domain.*;
import com.alphaone.logisticaRobots.domain.comportamiento.ComportamientoProvisionActiva;
import com.alphaone.logisticaRobots.domain.comportamiento.ComportamientoIntermedioBuffer;
import com.alphaone.logisticaRobots.domain.comportamiento.ComportamientoProvisionPasiva;
import com.alphaone.logisticaRobots.shared.ParametrosGenerales;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class Planificador { //lo está haciendo tomi
    private Grafo grafo; //tiene un mapa
    private List<Pedido> pedidos; //tiene una lista de pedidos que priorizar y laburar
    private Set<Robopuerto> robopuertos;
    private Set<CofreLogistico> cofres;
    private Set<RobotLogistico> robotsLogisticos;
    private final GrillaEspacial grillaEspacial;
    private Map<RobotLogistico, List<Punto>> rutasAsignadas; // Para evitar colisiones entre robots

    public static final double factorConsumo = ParametrosGenerales.FACTOR_CONSUMO;

    public Planificador(Set<Robopuerto> robopuertos, GrillaEspacial grillaEspacial, Set<CofreLogistico> cofres, Set<RobotLogistico> robotsLogisticos, List<Pedido> pedidos) {
        this.robopuertos = robopuertos;
        this.grillaEspacial = grillaEspacial;
        this.cofres = cofres;
        this.robotsLogisticos = robotsLogisticos;
        this.pedidos = pedidos;
        this.rutasAsignadas = new HashMap<>();
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
                    // Verificar si están en alcance.
                    // TODO: Revisar esta validacion tomando como premisa que debe verificar si un nodo tiene conexion posbile con otro
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
     * TODO: Corregir este método, no hace lo que dice.
     *  Analizar la posibilidad de usar un Warshall para esto, dado que la consulta será O(1) una vez que tenemos armada la matriz
     * Determina si dos puntos están conectados en la red logística.
     * Dos puntos están conectados si están dentro de la grilla espacial
     * y la distancia entre ellos es razonable para que un robot pueda recorrerla.
     *
     * @param origen Punto de origen
     * @param destino Punto de destino
     * @return true si los puntos están conectados, false en caso contrario
     */
    private boolean estanConectados(Punto origen, Punto destino) {
        // TODO: Eliminar esta validacion. La grilla es unica. Verificar que ambos puntos estén dentro de la grilla espacial
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
    public Map<Nodo, Ruta> calcularRutasOptimas(Nodo origen) { //TODO: Dijkstra comienza acá
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

    /**
     * Calcula la ruta más eficiente para cada pedido en la lista de pedidos.
     * Asigna los pedidos a los robots logísticos según la prioridad del pedido y las capacidades del robot.
     * Si un pedido falla, se crea un nuevo pedido con estado NUEVO y se intenta nuevamente hasta que
     * todos los pedidos sean satisfechos o se determine que no es posible satisfacerlos.
     * 
     * @return true si todos los pedidos pueden ser satisfechos, false en caso contrario
     */
    public boolean ejecutarRutas() {
        // Set para llevar registro de pedidos que ya han fallado y se han reintentado
        Set<String> pedidosReintentados = new HashSet<>();
        boolean algunPedidoFallido;
        boolean progresoRealizado;

        do {
            // Reiniciar las rutas asignadas
            rutasAsignadas.clear();

            // Lista para almacenar pedidos fallidos en esta iteración
            List<Pedido> pedidosFallidos = new ArrayList<>();

            // Ordenar pedidos por prioridad (ALTA > MEDIA > BAJA)
            List<Pedido> pedidosOrdenados = new ArrayList<>(pedidos);
            pedidosOrdenados.sort((p1, p2) -> {
                // Comparar por prioridad (mayor a menor)
                int comparacion = p2.getPrioridad().ordinal() - p1.getPrioridad().ordinal();
                // Si tienen la misma prioridad, mantener el orden original
                return comparacion != 0 ? comparacion : 0;
            });

            // Filtrar pedidos que no están completados ni fallidos
            pedidosOrdenados = pedidosOrdenados.stream()
                    .filter(p -> !p.estaCompletado() && !p.estaFallido())
                    .collect(Collectors.toList());

            // Si no hay pedidos pendientes, retornar true
            if (pedidosOrdenados.isEmpty()) {
                return true;
            }

            // Procesar cada pedido
            boolean todosPedidosSatisfechos = true;

            for (Pedido pedido : pedidosOrdenados) {
                // Si el pedido ya está en proceso, continuar con el siguiente
                if (pedido.getEstado() == Pedido.EstadoPedido.EN_PROCESO) {
                    continue;
                }

                // Buscar el mejor robot para este pedido
                RobotLogistico mejorRobot = encontrarMejorRobotParaPedido(pedido); //TODO: @tomi feli No encuentra el robot porque no encuentra el punto, metete adentro

                if (mejorRobot != null) {
                    // Asignar el pedido al robot
                    mejorRobot.agregarPedido(pedido);
                    pedido.marcarEnProceso();
                } else {
                    // No se encontró un robot adecuado para este pedido
                    todosPedidosSatisfechos = false;
                    pedido.marcarFallido();
                    pedidosFallidos.add(pedido);
                }
            }

            // Verificar si hay pedidos fallidos
            algunPedidoFallido = !pedidosFallidos.isEmpty();

            // Crear nuevos pedidos para los fallidos que no se han reintentado antes
            progresoRealizado = false;
            for (Pedido pedidoFallido : pedidosFallidos) {
                // Crear una clave única para este pedido
                String pedidoKey = pedidoFallido.getItem().getNombre() + "_" + 
                                  pedidoFallido.getCantidad() + "_" + 
                                  pedidoFallido.getCofreOrigen().getId() + "_" + 
                                  pedidoFallido.getCofreDestino().getId();

                // Si este pedido no se ha reintentado antes, crear uno nuevo
                if (!pedidosReintentados.contains(pedidoKey)) {
                    Pedido nuevoPedido = new Pedido(
                        pedidoFallido.getItem(),
                        pedidoFallido.getCantidad(),
                        pedidoFallido.getCofreOrigen(),
                        pedidoFallido.getCofreDestino(),
                        pedidoFallido.getPrioridad()
                    );

                    // Agregar el nuevo pedido a la lista
                    pedidos.add(nuevoPedido);

                    // Marcar este pedido como reintentado
                    pedidosReintentados.add(pedidoKey);

                    // Indicar que se ha realizado progreso
                    progresoRealizado = true;
                }
            }

            // Continuar el bucle si hay pedidos fallidos y se ha realizado algún progreso
        } while (algunPedidoFallido && progresoRealizado);

        // Verificar si todos los pedidos han sido satisfechos
        boolean todosSatisfechos = pedidos.stream()
                .noneMatch(p -> p.estaFallido());

        return todosSatisfechos;
    }

    /**
     * Encuentra el mejor robot para un pedido específico.
     * Considera la proximidad, capacidad de batería y disponibilidad del robot.
     * También verifica si hay colisiones con otros robots.
     * 
     * @param pedido El pedido a asignar
     * @return El mejor robot para el pedido, o null si no hay robots disponibles
     */
    private RobotLogistico encontrarMejorRobotParaPedido(Pedido pedido) {
        // Buscar el mejor cofre origen según el tipo de comportamiento
        CofreLogistico mejorOrigen = encontrarMejorCofreOrigen(pedido.getItem(), pedido.getCantidad());
        if (mejorOrigen == null) {
            return null; // No hay cofre origen adecuado
        }

        CofreLogistico destino = pedido.getCofreDestino();

        // Filtrar robots activos o en robopuertos (que pueden activarse)
        List<RobotLogistico> robotsDisponibles = robotsLogisticos.stream()
                .filter(r -> r.getEstado() == EstadoRobot.ACTIVO || 
                             r.getEstado() == EstadoRobot.PASIVO || 
                             r.getEstado() == EstadoRobot.CARGANDO)
                .collect(Collectors.toList());

        if (robotsDisponibles.isEmpty()) {
            return null;
        }

        // Encontrar robopuertos cercanos al cofre origen
        List<Robopuerto> robopuertosCercanos = new ArrayList<>();
        for (Robopuerto robopuerto : robopuertos) {
            if (robopuerto.estaEnCobertura(mejorOrigen.getPosicion())) {
                robopuertosCercanos.add(robopuerto);
            }
        }

        // Priorizar robots que están en robopuertos cercanos al cofre origen
        if (!robopuertosCercanos.isEmpty()) {
            List<RobotLogistico> robotsEnRobopuertosCercanos = robotsDisponibles.stream()
                    .filter(r -> robopuertosCercanos.stream()
                            .anyMatch(rp -> r.getPosicion().equals(rp.getPosicion())))
                    .collect(Collectors.toList());

            // Si hay robots en robopuertos cercanos, usar solo esos
            if (!robotsEnRobopuertosCercanos.isEmpty()) {
                robotsDisponibles = robotsEnRobopuertosCercanos;
            }
        }

        // Encontrar el nodo correspondiente al origen y destino
        Nodo nodoOrigen = encontrarNodo(mejorOrigen.getPosicion());
        Nodo nodoDestino = encontrarNodo(destino.getPosicion());

        if (nodoOrigen == null || nodoDestino == null) {
            return null; // No se encontraron los nodos en el grafo
        }

        // Evaluar cada robot
        RobotLogistico mejorRobot = null;
        double mejorPuntuacion = Double.MAX_VALUE;
        List<Punto> mejorRuta = null;

        for (RobotLogistico robot : robotsDisponibles) {
            // Encontrar el nodo correspondiente a la posición actual del robot
            Nodo nodoRobot = encontrarNodo(robot.getPosicion());

            if (nodoRobot == null) {
                continue; // No se encontró el nodo del robot en el grafo
            }

            // Calcular rutas óptimas desde la posición del robot
            Map<Nodo, Ruta> rutasDesdeRobot = calcularRutasOptimas(nodoRobot);

            // Verificar si hay ruta al origen
            if (!rutasDesdeRobot.containsKey(nodoOrigen)) {
                continue; // No hay ruta al origen
            }

            // Calcular rutas óptimas desde el origen
            Map<Nodo, Ruta> rutasDesdeOrigen = calcularRutasOptimas(nodoOrigen);

            // Verificar si hay ruta al destino
            if (!rutasDesdeOrigen.containsKey(nodoDestino)) {
                continue; // No hay ruta al destino
            }

            // Construir la ruta completa (robot -> origen -> destino)
            List<Punto> rutaCompleta = new ArrayList<>();

            // Agregar ruta de robot a origen
            Ruta rutaRobotOrigen = rutasDesdeRobot.get(nodoOrigen);
            if (rutaRobotOrigen != null) {
                rutaCompleta.add(rutaRobotOrigen.getPuntoInicio());
                rutaCompleta.add(rutaRobotOrigen.getPuntoFin());
            }

            // Agregar ruta de origen a destino
            Ruta rutaOrigenDestino = rutasDesdeOrigen.get(nodoDestino);
            if (rutaOrigenDestino != null && !rutaOrigenDestino.getPuntoFin().equals(rutaRobotOrigen.getPuntoFin())) {
                rutaCompleta.add(rutaOrigenDestino.getPuntoFin());
            }

            // Calcular distancia total
            double distanciaTotal = 0;
            for (int i = 0; i < rutaCompleta.size() - 1; i++) {
                distanciaTotal += rutaCompleta.get(i).distanciaHacia(rutaCompleta.get(i + 1));
            }

            // Calcular consumo de batería
            double consumoBateria = distanciaTotal * factorConsumo;

            // Verificar si el robot tiene suficiente batería
            if (!robot.tieneSuficienteBateria((int) Math.ceil(consumoBateria))) {
                // Verificar si hay un robopuerto cercano para recargar
                boolean puedeRecargar = verificarPosibilidadRecarga(robot, mejorOrigen, destino, consumoBateria);

                if (!puedeRecargar) {
                    continue; // No puede completar la misión con la batería actual
                }
            }

            // Verificar si hay colisiones con otros robots
            boolean hayColision = verificarColisiones(robot, rutaCompleta);
            if (hayColision) {
                continue; // Hay colisión con otro robot
            }

            // Calcular puntuación (menor es mejor)
            // Considerar: distancia, batería, capacidad de carga
            double puntuacion = distanciaTotal;

            // Ajustar puntuación según la batería disponible (penalizar baterías bajas)
            double porcentajeBateria = (double) robot.getBateriaActual() / robot.getBateriaMaxima();
            puntuacion = puntuacion / porcentajeBateria;

            // Verificar si este robot es mejor que el actual mejor
            if (puntuacion < mejorPuntuacion) {
                mejorRobot = robot;
                mejorPuntuacion = puntuacion;
                mejorRuta = rutaCompleta;
            }
        }

        // Si se encontró un robot adecuado, registrar su ruta para evitar colisiones
        if (mejorRobot != null && mejorRuta != null) {
            rutasAsignadas.put(mejorRobot, mejorRuta);
        }

        return mejorRobot;
    }

    /**
     * Encuentra el mejor cofre origen para un item según el tipo de comportamiento.
     * Prioriza cofres con ComportamientoProvisionActiva, luego ComportamientoIntermedioBuffer,
     * y finalmente ComportamientoProvisionPasiva.
     * 
     * @param item El item a buscar
     * @param cantidad La cantidad requerida
     * @return El mejor cofre origen, o null si no hay cofres adecuados
     */
    private CofreLogistico encontrarMejorCofreOrigen(Item item, int cantidad) {
        // Buscar cofres que puedan ofrecer el item
        List<CofreLogistico> cofresConItem = cofres.stream()
                .filter(c -> c.puedeOfrecer(item, cantidad))
                .collect(Collectors.toList());

        if (cofresConItem.isEmpty()) {
            return null;
        }

        //TODO: @Tomi Feli - REVISAR ESTO - No sé si tenga sentido
        // Buscar cofre con ComportamientoProvisionActiva
        for (CofreLogistico cofre : cofresConItem) {
            if (cofre.getTipoComportamiento(item).equals("Provision Activa")) {
                return cofre;
            }
        }

        // Buscar cofre con ComportamientoIntermedioBuffer
        for (CofreLogistico cofre : cofresConItem) {
            if (cofre.getTipoComportamiento(item).equals("Buffer Intermedio")) {
                return cofre;
            }
        }

        // Buscar cofre con ComportamientoProvisionPasiva
        for (CofreLogistico cofre : cofresConItem) {
            if (cofre.getTipoComportamiento(item).equals("Provision Pasiva")) {
                return cofre;
            }
        }

        // Si no se encontró ningún cofre con los comportamientos específicos, devolver el primero
        return cofresConItem.get(0);
    }

    /**
     * Verifica si hay colisiones con otros robots en la ruta especificada.
     * 
     * @param robot El robot a verificar
     * @param ruta La ruta a verificar
     * @return true si hay colisión, false en caso contrario
     */
    private boolean verificarColisiones(RobotLogistico robot, List<Punto> ruta) {
        // Verificar cada punto de la ruta
        for (Punto punto : ruta) {
            // Verificar si algún otro robot tiene este punto en su ruta
            for (Map.Entry<RobotLogistico, List<Punto>> entry : rutasAsignadas.entrySet()) {
                RobotLogistico otroRobot = entry.getKey();
                List<Punto> otraRuta = entry.getValue();

                // No verificar colisiones con el mismo robot
                if (otroRobot.equals(robot)) {
                    continue;
                }

                // Verificar si el punto está en la otra ruta
                if (otraRuta.contains(punto)) {
                    return true; // Hay colisión
                }
            }
        }

        return false; // No hay colisión
    }

    /**
     * Verifica si un robot puede recargar su batería en un robopuerto cercano para completar una misión.
     * 
     * @param robot El robot a verificar
     * @param origen El cofre de origen
     * @param destino El cofre de destino
     * @param consumoBateria El consumo de batería estimado para la misión
     * @return true si el robot puede recargar y completar la misión, false en caso contrario
     */
    private boolean verificarPosibilidadRecarga(RobotLogistico robot, CofreLogistico origen, CofreLogistico destino, double consumoBateria) {
        // Buscar robopuertos cercanos
        for (Robopuerto robopuerto : robopuertos) {
            // Calcular distancia al robopuerto
            double distanciaARobopuerto = robot.getPosicion().distanciaHacia(robopuerto.getPosicion());
            double consumoHastaRobopuerto = distanciaARobopuerto * factorConsumo;

            // Verificar si puede llegar al robopuerto
            if (robot.tieneSuficienteBateria((int) Math.ceil(consumoHastaRobopuerto))) {
                // Calcular distancia desde robopuerto a origen y luego a destino
                double distanciaRobopuertoOrigen = robopuerto.getPosicion().distanciaHacia(origen.getPosicion());
                double distanciaOrigenDestino = origen.getPosicion().distanciaHacia(destino.getPosicion());
                double consumoRestante = (distanciaRobopuertoOrigen + distanciaOrigenDestino) * factorConsumo;

                // Verificar si con batería completa puede completar la misión
                if (robot.getBateriaMaxima() >= consumoRestante) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Encuentra el nodo en el grafo que corresponde a la posición dada.
     * Si no encuentra una coincidencia exacta, busca el nodo más cercano.
     * 
     * @param posicion La posición a buscar
     * @return El nodo correspondiente, o null si no se encuentra
     */
    private Nodo encontrarNodo(Punto posicion) {
        // Primero intentar encontrar una coincidencia exacta
        for (Nodo nodo : grafo.getNodos()) {
            if (nodo.getNodo().equals(posicion)) {
                return nodo;
            }
        }

        // Si no hay coincidencia exacta, buscar el nodo más cercano
        Nodo nodoMasCercano = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (Nodo nodo : grafo.getNodos()) {
            double distancia = nodo.getNodo().distanciaHacia(posicion);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                nodoMasCercano = nodo;
            }
        }

        return nodoMasCercano;
    }
}
