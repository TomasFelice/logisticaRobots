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

        // Crear un nodo por cada celda de la grilla espacial (dentro del rectángulo)
        Map<Punto, Nodo> mapaNodos = new HashMap<>();
        Punto origen = grillaEspacial.getOrigen();
        int x0 = origen.getX();
        int y0 = origen.getY();
        int ancho = grillaEspacial.getAncho();
        int alto = grillaEspacial.getAlto();

        // Primero, identificar las posiciones ocupadas por cofres, robots y robopuertos
        Set<Punto> posicionesCofres = cofres.stream().map(CofreLogistico::getPosicion).collect(Collectors.toSet());
        Set<Punto> posicionesRobots = robotsLogisticos.stream().map(RobotLogistico::getPosicion).collect(Collectors.toSet());
        Map<Punto, RobotLogistico> robotsPorPosicion = robotsLogisticos.stream().collect(Collectors.toMap(RobotLogistico::getPosicion, r -> r));
        Set<Punto> posicionesRobopuertos = robopuertos.stream().map(Robopuerto::getPosicion).collect(Collectors.toSet());

        // Crear nodos para cada celda válida
        for (int dx = 0; dx < ancho; dx++) {
            for (int dy = 0; dy < alto; dy++) {
                Punto p = new Punto(x0 + dx, y0 + dy);
                if (grillaEspacial.dentroDeGrilla(p)) {
                    Nodo nodo = new Nodo(p);
                    grafo.agregarNodo(nodo);
                    mapaNodos.put(p, nodo);
                }
            }
        }

        // Conectar nodos ortogonalmente si ambas celdas son transitables
        int[][] ortogonales = { {1,0}, {-1,0}, {0,1}, {0,-1} };
        for (Nodo nodo : grafo.getNodos()) {
            Punto p = nodo.getNodo();
            for (int[] dir : ortogonales) {
                Punto vecino = new Punto(p.getX() + dir[0], p.getY() + dir[1]);
                Nodo nodoVecino = mapaNodos.get(vecino);
                if (nodoVecino != null && esTransitable(vecino, null, posicionesCofres, posicionesRobots, robotsPorPosicion, posicionesRobopuertos)) {
                    double peso = p.distanciaHacia(vecino) * factorConsumo;
                    nodo.agregarArista(new Arista(nodo, nodoVecino, peso));
                }
            }
        }
    }

    /**
     * Determina si una celda es transitable para un robot dado.
     * Si robotPlanificador es null, se asume para construcción general del grafo.
     */
    private boolean esTransitable(Punto punto, RobotLogistico robotPlanificador, Set<Punto> posicionesCofres, Set<Punto> posicionesRobots, Map<Punto, RobotLogistico> robotsPorPosicion, Set<Punto> posicionesRobopuertos) {
        // Cofres: la celda es obstáculo
        if (posicionesCofres.contains(punto)) return false;
        // Robots: la celda es obstáculo, salvo que sea el propio robot planificando
        if (posicionesRobots.contains(punto)) {
            if (robotPlanificador == null) return false;
            RobotLogistico robotEnCelda = robotsPorPosicion.get(punto);
            if (robotEnCelda != null && !robotEnCelda.equals(robotPlanificador)) return false;
        }
        // Robopuertos: obstáculo solo si hay un robot ACTIVO en la celda
        if (posicionesRobopuertos.contains(punto)) {
            RobotLogistico robotEnRobopuerto = robotsPorPosicion.get(punto);
            if (robotEnRobopuerto != null && robotEnRobopuerto.getEstado() == EstadoRobot.ACTIVO) {
                if (robotPlanificador == null || !robotEnRobopuerto.equals(robotPlanificador)) return false;
            }
        }
        return true;
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
                // Si tienen la misma prioridad, mantener el orden original
                return p2.getPrioridad().ordinal() - p1.getPrioridad().ordinal();
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
                RobotLogistico mejorRobot = encontrarMejorRobotParaPedido(pedido);

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
     * Devuelve la lista de celdas ortogonalmente adyacentes y transitables a un punto dado (por ejemplo, un cofre).
     */
    private List<Nodo> obtenerAdyacentesTransitables(Punto punto, RobotLogistico robotPlanificador, Set<Punto> posicionesCofres, Set<Punto> posicionesRobots, Map<Punto, RobotLogistico> robotsPorPosicion, Set<Punto> posicionesRobopuertos) {
        int[][] ortogonales = { {1,0}, {-1,0}, {0,1}, {0,-1} };
        List<Nodo> adyacentes = new ArrayList<>();
        for (int[] dir : ortogonales) {
            Punto ady = new Punto(punto.getX() + dir[0], punto.getY() + dir[1]);
            Nodo nodoAdy = encontrarNodo(ady);
            if (nodoAdy != null && esTransitable(ady, robotPlanificador, posicionesCofres, posicionesRobots, robotsPorPosicion, posicionesRobopuertos)) {
                adyacentes.add(nodoAdy);
            }
        }
        return adyacentes;
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

        // Verificar que el destino también está dentro del alcance de algún robopuerto
        boolean destinoAccesible = robopuertos.stream()
                .anyMatch(robopuerto -> robopuerto.estaEnCobertura(destino.getPosicion()));
        
        if (!destinoAccesible) {
            return null; // El destino no está dentro del alcance de ningún robopuerto
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

        // Obtener sets y mapas de obstáculos actualizados
        Set<Punto> posicionesCofres = cofres.stream().map(CofreLogistico::getPosicion).collect(Collectors.toSet());
        Set<Punto> posicionesRobots = robotsLogisticos.stream().map(RobotLogistico::getPosicion).collect(Collectors.toSet());
        Map<Punto, RobotLogistico> robotsPorPosicion = robotsLogisticos.stream().collect(Collectors.toMap(RobotLogistico::getPosicion, r -> r));
        Set<Punto> posicionesRobopuertos = robopuertos.stream().map(Robopuerto::getPosicion).collect(Collectors.toSet());

        // Encontrar nodos adyacentes transitables al origen y destino (cofres)
        List<Nodo> nodosAdyacentesOrigen = obtenerAdyacentesTransitables(mejorOrigen.getPosicion(), null, posicionesCofres, posicionesRobots, robotsPorPosicion, posicionesRobopuertos);
        List<Nodo> nodosAdyacentesDestino = obtenerAdyacentesTransitables(destino.getPosicion(), null, posicionesCofres, posicionesRobots, robotsPorPosicion, posicionesRobopuertos);

        if (nodosAdyacentesOrigen.isEmpty() || nodosAdyacentesDestino.isEmpty()) {
            return null; // No hay acceso al cofre origen o destino
        }

        // Evaluar cada robot
        RobotLogistico mejorRobot = null;
        double mejorPuntuacion = Double.MAX_VALUE;
        List<Punto> mejorRuta = null;

        for (RobotLogistico robot : robotsDisponibles) {
            Nodo nodoRobot = encontrarNodo(robot.getPosicion());
            if (nodoRobot == null) {
                continue;
            }

            // Buscar la mejor ruta a cualquier adyacente al origen
            Map<Nodo, Ruta> rutasDesdeRobot = calcularRutasOptimas(nodoRobot);
            Nodo mejorNodoAdyOrigen = null;
            Ruta mejorRutaRobotOrigen = null;
            double mejorDistanciaRobotOrigen = Double.MAX_VALUE;
            for (Nodo nodoAdy : nodosAdyacentesOrigen) {
                Ruta ruta = rutasDesdeRobot.get(nodoAdy);
                if (ruta != null) {
                    double dist = nodoRobot.getNodo().distanciaHacia(nodoAdy.getNodo());
                    if (dist < mejorDistanciaRobotOrigen) {
                        mejorDistanciaRobotOrigen = dist;
                        mejorNodoAdyOrigen = nodoAdy;
                        mejorRutaRobotOrigen = ruta;
                    }
                }
            }
            if (mejorRutaRobotOrigen == null) continue;

            // Buscar la mejor ruta desde adyacente al origen a cualquier adyacente al destino
            Map<Nodo, Ruta> rutasDesdeOrigen = calcularRutasOptimas(mejorNodoAdyOrigen);
            Nodo mejorNodoAdyDestino = null;
            Ruta mejorRutaOrigenDestino = null;
            double mejorDistanciaOrigenDestino = Double.MAX_VALUE;
            for (Nodo nodoAdy : nodosAdyacentesDestino) {
                Ruta ruta = rutasDesdeOrigen.get(nodoAdy);
                if (ruta != null) {
                    double dist = mejorNodoAdyOrigen.getNodo().distanciaHacia(nodoAdy.getNodo());
                    if (dist < mejorDistanciaOrigenDestino) {
                        mejorDistanciaOrigenDestino = dist;
                        mejorNodoAdyDestino = nodoAdy;
                        mejorRutaOrigenDestino = ruta;
                    }
                }
            }
            if (mejorRutaOrigenDestino == null) continue;

            // Construir la ruta completa (robot -> adyacente origen -> adyacente destino)
            List<Punto> rutaCompleta = new ArrayList<>();
            rutaCompleta.add(mejorRutaRobotOrigen.getPuntoInicio());
            rutaCompleta.add(mejorRutaRobotOrigen.getPuntoFin());
            if (!mejorRutaOrigenDestino.getPuntoFin().equals(mejorRutaRobotOrigen.getPuntoFin())) {
                rutaCompleta.add(mejorRutaOrigenDestino.getPuntoFin());
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
                boolean puedeRecargar = verificarPosibilidadRecarga(robot, mejorOrigen, destino, consumoBateria);
                if (!puedeRecargar) {
                    continue;
                }
            }

            // Verificar si hay colisiones con otros robots
            boolean hayColision = verificarColisiones(robot, rutaCompleta);
            if (hayColision) {
                continue;
            }

            // Verificar que toda la ruta esté dentro del alcance de algún robopuerto
            if (!rutaDentroDelAlcance(rutaCompleta)) {
                continue;
            }

            // Calcular puntuación (menor es mejor)
            double puntuacion = distanciaTotal;
            double porcentajeBateria = (double) robot.getBateriaActual() / robot.getBateriaMaxima();
            puntuacion = puntuacion / porcentajeBateria;

            if (puntuacion < mejorPuntuacion) {
                mejorRobot = robot;
                mejorPuntuacion = puntuacion;
                mejorRuta = rutaCompleta;
            }
        }

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
     * También verifica que el robot no se salga del alcance de los robopuertos.
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

                // Verificar que el origen y destino están dentro del alcance del robopuerto
                if (!robopuerto.estaEnCobertura(origen.getPosicion()) || !robopuerto.estaEnCobertura(destino.getPosicion())) {
                    continue; // Este robopuerto no puede cubrir la misión
                }

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

    /**
     * Verifica si todos los puntos de una ruta están dentro del alcance de algún robopuerto.
     * 
     * @param ruta La ruta a verificar
     * @return true si todos los puntos están dentro del alcance, false en caso contrario
     */
    private boolean rutaDentroDelAlcance(List<Punto> ruta) {
        for (Punto punto : ruta) {
            boolean puntoAccesible = robopuertos.stream()
                    .anyMatch(robopuerto -> robopuerto.estaEnCobertura(punto));
            if (!puntoAccesible) {
                return false;
            }
        }
        return true;
    }
}
