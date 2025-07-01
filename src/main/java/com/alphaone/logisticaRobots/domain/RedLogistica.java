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
    public void simularCiclo() {
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
                // Procesar el pedido actual del robot
                robot.procesarSiguientePedido();
                System.out.println("Robot " + robot + " procesando pedido");
            } else if (robot.getEstado() == EstadoRobot.ACTIVO) {
                // Si el robot está activo, intentar procesar un nuevo pedido
                if (robot.procesarSiguientePedido()) {
                    System.out.println("Robot " + robot + " iniciando nuevo pedido");
                }
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

        // Verificar si hay robots en movimiento
        // TODO: Validar que los estados del robot sean correctos para validar si es un estado estable o no.
        boolean robotsEnMovimiento = robotsLogisticos.stream()
                .anyMatch(r -> r.getEstado() == EstadoRobot.EN_MISION);

        // Si no hay pedidos pendientes ni robots en movimiento, el sistema está estable
        return !pedidosPendientes && !robotsEnMovimiento;
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
