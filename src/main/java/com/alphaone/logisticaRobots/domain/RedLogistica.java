package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.GrillaEspacial;
import com.alphaone.logisticaRobots.domain.pathfinding.Planificador;
import com.alphaone.logisticaRobots.domain.pathfinding.Punto;

import java.util.*;
import java.util.stream.Collectors;

public class RedLogistica { // es el universo donde se componen las cosas

    private Set<Robopuerto> robopuertos;
    private Set<CofreLogistico> cofres;
    private Set<RobotLogistico> robotsLogisticos;
    private List<Pedido> pedidos;
    private final GrillaEspacial grillaEspacial;
    private final Planificador planificador;

    /**
     * Constructor por defecto que inicializa una red logística vacía.
     */
    public RedLogistica() {
        this.robopuertos = new HashSet<>();
        this.cofres = new HashSet<>();
        this.robotsLogisticos = new HashSet<>();
        this.pedidos = new ArrayList<>();
        this.grillaEspacial = new GrillaEspacial(new Punto(0, 0), 100, 100); // Origen en (0,0) con ancho 100 y alto 100
        this.planificador = new Planificador(robopuertos, grillaEspacial, cofres, robotsLogisticos, pedidos);
    }

    public RedLogistica(Set<Robopuerto> robopuertos, GrillaEspacial grillaEspacial, Set<CofreLogistico> cofres, Set<RobotLogistico> robotsLogisticos, List<Pedido> pedidos) {
        this.robopuertos = robopuertos;
        this.grillaEspacial = grillaEspacial;
        this.cofres = cofres;
        this.robotsLogisticos = robotsLogisticos;
        this.pedidos = pedidos;
        this.planificador = new Planificador(robopuertos, grillaEspacial, cofres, robotsLogisticos, pedidos);
    }

    public Set<Robopuerto> getRobopuertos() {
        return robopuertos;
    }

    public GrillaEspacial getGrillaEspacial() {
        return grillaEspacial;
    }

    public Set<CofreLogistico> getCofres() {
        return cofres;
    }

    public Set<RobotLogistico> getRobotsLogisticos() {
        return robotsLogisticos;
    }

    public List<Pedido> getPedidos() {
        return pedidos;
    }

    public void agregarRobopuerto(Robopuerto robopuerto) {
        this.robopuertos.add(robopuerto);
    }

    public void eliminarRobopuerto(Robopuerto robopuerto) {
        this.robopuertos.remove(robopuerto);
    }

    public void agregarCofre(CofreLogistico cofre) {
        this.cofres.add(cofre);
    }

    public void eliminarCofre(CofreLogistico cofre) {
        this.cofres.remove(cofre);
    }

    public void agregarRobot(RobotLogistico robot) {
        this.robotsLogisticos.add(robot);
        robot.setRedLogistica(this);
    }

    public void eliminarRobot(RobotLogistico robot) {
        this.robotsLogisticos.remove(robot);
    }

    public void agregarPedido(Pedido pedido) {
        this.pedidos.add(pedido);
    }

    public void eliminarPedido(Pedido pedido) {
        this.pedidos.remove(pedido);
    }

    public boolean estaVacia() {
        return
            robopuertos.isEmpty()
            && cofres.isEmpty()
            && robotsLogisticos.isEmpty()
            && pedidos.isEmpty();
    }

    /**
     * Simula un ciclo de movimiento de todos los robots de la red.
     * Utiliza el Planificador para calcular las rutas más eficientes para los pedidos
     * y asignarlos a los robots según su prioridad y capacidades.
     * Procesa los pedidos pendientes y mueve los robots según sea necesario.
     */
    public void simularCiclo(int cicloActual) {
        // Verificar que todos los robots tengan la red configurada
        verificarConfiguracionRobots();
        
        // Utilizar el planificador para calcular las rutas más eficientes y asignar pedidos a robots
        boolean todosPedidosSatisfechos = planificador.ejecutarRutas();

        if (todosPedidosSatisfechos) {
            System.out.println("Todos los pedidos han sido satisfechos o están en proceso");
        } else {
            System.out.println("Algunos pedidos no pudieron ser satisfechos");
        }

        // Procesar robots según su estado actual
        for (RobotLogistico robot : robotsLogisticos) {
            if (robot.getEstado() == EstadoRobot.EN_MISION) {
                robot.procesarSiguientePedido(cicloActual);
                System.out.println("Robot " + robot + " procesando pedido");
                // Si el robot NO tiene pedido actual ni pendientes, y está en un robopuerto, cambiar a PASIVO
                boolean sinPedidos = robot.getPedidosPendientes().isEmpty() && robot.getHistorialPedidos().size() > 0 && robot.getEstado() == EstadoRobot.EN_MISION && robot.getRutaActual().isEmpty();
                boolean enRobopuerto = robopuertos.stream().anyMatch(rp -> robot.getPosicion().equals(rp.getPosicion()));
                if (sinPedidos && enRobopuerto) {
                    robot.cambiarEstado(EstadoRobot.PASIVO);
                    System.out.println("Robot " + robot + " llegó a robopuerto y pasa a PASIVO");
                }
            } else if (robot.getEstado() == EstadoRobot.ACTIVO) {
                robot.procesarSiguientePedido(cicloActual);
                System.out.println("Robot " + robot + " iniciando nuevo pedido");
            } else if (robot.getEstado() == EstadoRobot.PASIVO || robot.getEstado() == EstadoRobot.CARGANDO) {
                // Si el robot está en un robopuerto, intentamos recargarlo
                for (Robopuerto robopuerto : robopuertos) {
                    if (robot.getPosicion().equals(robopuerto.getPosicion())) {
                        try {
                            robopuerto.recargarRobot(robot);
                            System.out.println("Robot " + robot + " recargando en " + robopuerto);

                            // Si la batería está llena, cambiar a estado activo
                            if (robot.getBateriaActual() >= robot.getBateriaMaxima()) {
                                robot.cambiarEstado(EstadoRobot.ACTIVO);
                            }
                        } catch (IllegalStateException e) {
                            // El robot ya está cargado o no está en estado válido
                            System.out.println("No se pudo recargar el robot: " + e.getMessage());
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Verifica que todos los robots tengan la red logística configurada.
     * Si algún robot no la tiene, la configura automáticamente.
     */
    private void verificarConfiguracionRobots() {
        for (RobotLogistico robot : robotsLogisticos) {
            if (!robot.tieneRedLogistica()) {
                System.out.println("Configurando red logística para robot " + robot.getId());
                robot.setRedLogistica(this);
            }
        }
    }

    /**
     * Indica que ya cumplió con todos los pedidos y no tiene más
     * Movimientos pendientes
     *
     * @return boolean
     */
    public boolean haAlcanzadoEstadoEstable() {
        // Verificar si hay pedidos pendientes
        boolean pedidosPendientes = pedidos.stream()
                .anyMatch(p -> p.getEstado() == Pedido.EstadoPedido.NUEVO || p.getEstado() == Pedido.EstadoPedido.EN_PROCESO);

        // Verificar que todos los robots estén en un robopuerto y en estado PASIVO, CARGANDO o ACTIVO
        boolean todosRobotsEstables = robotsLogisticos.stream().allMatch(
            robot ->
                (robopuertos.stream().anyMatch(rp -> robot.getPosicion().equals(rp.getPosicion()))) &&
                (robot.getEstado() == EstadoRobot.PASIVO || robot.getEstado() == EstadoRobot.CARGANDO || robot.getEstado() == EstadoRobot.ACTIVO)
        );

        // Verificar si hay cofres inaccesibles que impiden completar pedidos
        boolean hayCofresInaccesibles = hayCofresInaccesiblesQueImpidenCompletarPedidos();

        // El sistema está estable solo si no hay pedidos pendientes, todos los robots están en robopuerto y en estado estable,
        // Y no hay cofres inaccesibles que impidan completar pedidos
        return !pedidosPendientes && todosRobotsEstables && !hayCofresInaccesibles;
    }

    /**
     * Verifica si hay cofres inaccesibles que impiden completar pedidos
     * @return true si hay cofres inaccesibles que afectan pedidos pendientes
     */
    public boolean hayCofresInaccesiblesQueImpidenCompletarPedidos() {
        // Obtener todos los cofres inaccesibles
        List<CofreLogistico> cofresInaccesibles = getCofresInaccesibles();
        
        if (cofresInaccesibles.isEmpty()) {
            return false;
        }
        
        // Verificar si algún pedido pendiente involucra cofres inaccesibles
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == Pedido.EstadoPedido.NUEVO || pedido.getEstado() == Pedido.EstadoPedido.EN_PROCESO) {
                CofreLogistico origen = pedido.getCofreOrigen();
                CofreLogistico destino = pedido.getCofreDestino();
                
                // Si el origen o destino es inaccesible, el pedido no se puede completar
                if (cofresInaccesibles.contains(origen) || cofresInaccesibles.contains(destino)) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Obtiene la lista de cofres inaccesibles
     * @return Lista de cofres que no pueden ser alcanzados por ningún robopuerto
     */
    public List<CofreLogistico> getCofresInaccesibles() {
        return cofres.stream()
                .filter(cofre -> !esCofreAccesible(cofre))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene información detallada sobre cofres inaccesibles para el logging
     * @return String con información detallada de cofres inaccesibles
     */
    public String getInformacionCofresInaccesibles() {
        List<CofreLogistico> cofresInaccesibles = getCofresInaccesibles();
        
        if (cofresInaccesibles.isEmpty()) {
            return "No hay cofres inaccesibles.";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("COFRES INACCESIBLES DETECTADOS:\n");
        info.append("-".repeat(40)).append("\n");
        
        for (CofreLogistico cofre : cofresInaccesibles) {
            info.append("Cofre ID: ").append(cofre.getId()).append("\n");
            info.append("  Posición: ").append(cofre.getPosicion()).append("\n");
            info.append("  Tipo: ").append(cofre.getTipoComportamientoDefecto()).append("\n");
            
            // Verificar si está fuera de la grilla
            if (!grillaEspacial.dentroDeGrilla(cofre.getPosicion())) {
                info.append("  Razón: Fuera de los límites de la grilla espacial\n");
            } else {
                info.append("  Razón: No está en cobertura de ningún robopuerto\n");
            }
            info.append("\n");
        }
        
        // Verificar pedidos afectados
        info.append("PEDIDOS AFECTADOS POR COFRES INACCESIBLES:\n");
        info.append("-".repeat(45)).append("\n");
        
        boolean hayPedidosAfectados = false;
        for (Pedido pedido : pedidos) {
            if (pedido.getEstado() == Pedido.EstadoPedido.NUEVO || pedido.getEstado() == Pedido.EstadoPedido.EN_PROCESO) {
                CofreLogistico origen = pedido.getCofreOrigen();
                CofreLogistico destino = pedido.getCofreDestino();
                
                if (cofresInaccesibles.contains(origen) || cofresInaccesibles.contains(destino)) {
                    hayPedidosAfectados = true;
                    info.append("Pedido: ").append(pedido.getItem().getNombre()).append(" x").append(pedido.getCantidad()).append("\n");
                    info.append("  Origen: ").append(origen.getId()).append(" (").append(origen.getPosicion()).append(")");
                    if (cofresInaccesibles.contains(origen)) {
                        info.append(" [INACCESIBLE]");
                    }
                    info.append("\n");
                    info.append("  Destino: ").append(destino.getId()).append(" (").append(destino.getPosicion()).append(")");
                    if (cofresInaccesibles.contains(destino)) {
                        info.append(" [INACCESIBLE]");
                    }
                    info.append("\n\n");
                }
            }
        }
        
        if (!hayPedidosAfectados) {
            info.append("No hay pedidos afectados por cofres inaccesibles.\n");
        }
        
        return info.toString();
    }

    /**
     * Indica si el cofre es accesible o no
     *
     * @param cofre CofreLogistico
     * @return boolean
     */
    public boolean esCofreAccesible(CofreLogistico cofre) {
        // TODO: REvisar esta validacion, creo que no va. Verificar si el cofre está dentro de la grilla espacial
        if (!grillaEspacial.dentroDeGrilla(cofre.getPosicion())) {
            return false;
        }

        // Verificar si hay al menos un robopuerto que pueda alcanzar el cofre
        for (Robopuerto robopuerto : robopuertos) {
            if (robopuerto.estaEnCobertura(cofre.getPosicion())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Encuentra el robopuerto más cercano a una posición dada
     *
     * @param posicion Punto donde se quiere encontrar el robopuerto más cercano
     * @return Robopuerto más cercano, o null si no hay robopuertos
     */
    public Robopuerto getRobopuertoMasCercano(Punto posicion) {
        if (robopuertos.isEmpty()) {
            return null;
        }

        Robopuerto masCercano = null;
        double distanciaMinima = Double.MAX_VALUE;

        for (Robopuerto robopuerto : robopuertos) {
            double distancia = robopuerto.getPosicion().distanciaHacia(posicion);
            if (distancia < distanciaMinima) {
                distanciaMinima = distancia;
                masCercano = robopuerto;
            }
        }

        return masCercano;
    }

    /**
     * Obtiene los cofres que están dentro del área de cobertura de un robopuerto específico
     *
     * @param robopuerto Robopuerto del cual se quieren obtener los cofres en cobertura
     * @return Lista de cofres que están dentro del área de cobertura del robopuerto
     */
    public List<CofreLogistico> getCofresEnCobertura(Robopuerto robopuerto) {
        return cofres.stream()
                .filter(cofre -> robopuerto.estaEnCobertura(cofre.getPosicion()))
                .collect(Collectors.toList());
    }

    public RobotLogistico buscarRobotPorId(String idRobot) {
        if (idRobot == null) {
            return null;
        }

        return robotsLogisticos.stream()
                .filter(robot -> idRobot.equals(String.valueOf(robot.getId())))
                .findFirst()
                .orElse(null);
    }

    public CofreLogistico buscarCofrePorId(String idEntidad) {
        if (idEntidad == null) {
            return null;
        }

        return cofres.stream()
                .filter(cofre -> idEntidad.equals(cofre.getId()))
                .findFirst()
                .orElse(null);
    }

    public Robopuerto buscarRobopuertoPorId(String idEntidad) {
        if (idEntidad == null) {
            return null;
        }

        return robopuertos.stream()
                .filter(robopuerto -> idEntidad.equals(robopuerto.getId()))
                .findFirst()
                .orElse(null);
    }
}
