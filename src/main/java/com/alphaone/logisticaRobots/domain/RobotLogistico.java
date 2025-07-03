package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.Punto;
import com.alphaone.logisticaRobots.shared.ParametrosGenerales;
import com.alphaone.logisticaRobots.infrastructure.logging.LoggerMovimientosRobots;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;

public class RobotLogistico implements Ubicable {
    private final int id;
    private Punto posicion;
    private final int bateriaMaxima;
    private int bateriaActual;
    private final static int capacidadPedidosTraslado = 10; // Los robots transportan una cantidad predefinida de ítems en cada viaje. Esta cantidad debe ser configurable a nivel global al momento de correr la simulación.
    private EstadoRobot estado;
    private Map<Item, Integer> cargaActual;  // Los ítems que está transportando y su cantidad
    private final Robopuerto robopuertoBase; // el robot empieza en un nodo del camino y puede desplazarse a otro en base a los nodos, sus pesos y cantidad de robots que se tenga
    private final Queue<Pedido> pedidosPendientes = new LinkedList<>();
    private final List<Pedido> historialPedidos = new ArrayList<>();

    private Pedido pedidoActual;
    private RedLogistica redLogistica; // Referencia a la red logística para verificaciones

    public RobotLogistico(int id, Punto posicion, Robopuerto robopuertoBase, int bateriaMaxima, int capacidadPedidosTraslado) {
        this.id = id;
        this.posicion = Objects.requireNonNull(posicion, "Posición no puede ser null");
        this.bateriaMaxima = validarBateria(bateriaMaxima);
        this.bateriaActual = this.bateriaMaxima;  // Inicia con la batería llena
        this.estado = EstadoRobot.ACTIVO; // com.alphaone.logisticaRobots.domain.Estado inicial
        this.cargaActual = new HashMap<>();  // Debemos inicializarlo con la carga máxima. PENDIENTE (Deberíamos hacer un inventarios también?
        this.robopuertoBase = Objects.requireNonNull(robopuertoBase); // de algún lado tiene que empezar el robot
    }

    //Getters:
    public int getId() {return id;}
    public Robopuerto getRobopuertoBase() {return robopuertoBase;}
    public Map<Item, Integer> getCargaActual() {return cargaActual;}
    public int getCantidadPedidosEnProceso() {return (pedidoActual != null) ? 1 : 0;}

    public int getBateriaActual() {return bateriaActual;}
    public int getBateriaMaxima() {return bateriaMaxima;}
    public int getCapacidadCarga() {return capacidadPedidosTraslado;}

    @Override
    public Punto getPosicion() {
        return posicion;
    }

    @Override
    public void setPosicion(Punto posicion) {
        this.posicion = posicion;
    }

    public EstadoRobot getEstado() {return estado;}
    public List<Pedido> getHistorialPedidos() { return historialPedidos; }
    public Queue<Pedido> getPedidosPendientes() { return pedidosPendientes; }

    /**
     * Establece la referencia a la red logística para verificaciones de colisiones y alcance.
     * 
     * @param redLogistica La red logística
     */
    public void setRedLogistica(RedLogistica redLogistica) {
        this.redLogistica = redLogistica;
        System.out.println("Robot " + id + ": Red logística configurada");
    }

    /**
     * Verifica si la red logística está configurada para este robot.
     * 
     * @return true si está configurada, false en caso contrario
     */
    public boolean tieneRedLogistica() {
        return redLogistica != null;
    }

    public void agregarPedido(Pedido pedido) {
        if (pedido == null) throw new IllegalArgumentException("El pedido no puede ser null");
        pedidosPendientes.add(pedido);
    }

    private void finalizarPedido() {
        historialPedidos.add(pedidoActual);
        pedidoActual = null;
        // Si hay más pedidos pendientes, el robot seguirá en EN_MISION cuando tome el siguiente pedido
        // Si NO hay más pedidos pendientes, el robot debe volver a un robopuerto y permanecer en EN_MISION hasta llegar
        if (!pedidosPendientes.isEmpty()) {
            cambiarEstado(EstadoRobot.ACTIVO); // Puede tomar otro pedido
        } else {
            // No cambiar a ACTIVO, queda en EN_MISION hasta llegar a un robopuerto
            // El cambio a PASIVO se hará en RedLogistica.simularCiclo
        }
    }

    /**
     * Procesa el siguiente pedido pendiente.
     * Este método debe ser llamado en cada ciclo de simulación para que el robot
     * avance en la ejecución de sus pedidos.
     * 
     * @return true si se procesó un pedido, false si no había pedidos pendientes
     */
    public boolean procesarSiguientePedido(int cicloActual) {
        // Si ya hay un pedido en proceso, continuar con él
        if (pedidoActual != null) {
            return continuarPedidoActual(cicloActual);
        }

        // Si no hay pedido actual pero hay pendientes, tomar el siguiente
        if (!pedidosPendientes.isEmpty()) {
            pedidoActual = pedidosPendientes.poll();
            pedidoActual.marcarEnProceso();
            cambiarEstado(EstadoRobot.EN_MISION);
            return true;
        }

        // Si no hay pedidos pendientes ni en proceso, pero el robot está en EN_MISION, debe volver a un robopuerto
        if (getEstado() == EstadoRobot.EN_MISION && redLogistica != null) {
            boolean enRobopuerto = redLogistica.getRobopuertos().stream().anyMatch(rp -> getPosicion().equals(rp.getPosicion()));
            if (!enRobopuerto) {
                Robopuerto robopuertoCercano = redLogistica.getRobopuertoMasCercano(getPosicion());
                if (robopuertoCercano != null) {
                    Punto destinoFinal = robopuertoCercano.getPosicion();
                    Punto siguientePaso = calcularSiguientePaso(getPosicion(), destinoFinal);
                    if (siguientePaso != null && esMovimientoValido(siguientePaso, destinoFinal)) {
                        setPosicion(siguientePaso);
                        int consumoBateria = (int) Math.ceil(getPosicion().distanciaHacia(siguientePaso) * ParametrosGenerales.FACTOR_CONSUMO);
                        try {
                            consumirBateria(consumoBateria);
                            System.out.println("Robot " + id + " volviendo a robopuerto. Posición: " + getPosicion() + ", Batería: " + bateriaActual + "/" + bateriaMaxima);
                        } catch (IllegalStateException e) {
                            // Si no puede moverse, simplemente no hace nada este ciclo
                        }
                    }
                }
            }
        }
        // No hay pedidos para procesar
        return false;
    }

    /**
     * Verifica si dos puntos son ortogonalmente adyacentes
     */
    private boolean esAdyacente(Punto a, Punto b) {
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        return (dx == 1 && dy == 0) || (dx == 0 && dy == 1);
    }

    /**
     * Continúa con el procesamiento del pedido actual.
     * El robot se mueve 1 casillero por ciclo de ejecución.
     * No se solapa con otros robots, robopuertos o cofres.
     * No se sale del alcance de un robopuerto.
     * 
     * @return true si el pedido sigue en proceso, false si se completó o falló
     */
    private boolean continuarPedidoActual(int cicloActual) {
        if (pedidoActual == null) {
            return false;
        }
        // Verificar que la red logística esté configurada
        if (!tieneRedLogistica()) {
            LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "error_red_logistica", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
            System.out.println("Robot " + id + ": ERROR - No hay red logística configurada. No se pueden verificar colisiones ni alcance.");
            pedidoActual.marcarFallido();
            finalizarPedido();
            return false;
        }
        if (!tieneSuficienteBateria(10)) {
            LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "espera_recarga", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
            cambiarEstado(EstadoRobot.PASIVO);
            System.out.println("Robot " + id + " necesita recargar. Batería actual: " + bateriaActual);
            return true;
        }
        CofreLogistico origen = pedidoActual.getCofreOrigen();
        CofreLogistico destino = pedidoActual.getCofreDestino();
        Item item = pedidoActual.getItem();
        int cantidad = pedidoActual.getCantidad();
        // 1. Moverse a una celda adyacente al cofre de origen
        if (!cargaActual.containsKey(item)) {
            if (!esAdyacente(posicion, origen.getPosicion())) {
                if (posicion.distanciaHacia(origen.getPosicion()) <= 1.5) {
                    Punto posicionAdyacente = encontrarPosicionAdyacenteLibre(origen.getPosicion());
                    if (posicionAdyacente != null) {
                        LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "mover_adjacente_origen", posicion.toString(), posicionAdyacente.toString(), posicion.distanciaHacia(posicionAdyacente), bateriaActual + "/" + bateriaMaxima);
                        setPosicion(posicionAdyacente);
                        System.out.println("Robot " + id + " llegó a una celda adyacente al cofre de origen " + origen.getId());
                        consumirBateria(2);
                    } else {
                        LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "fallo_adjacente_origen", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                        System.out.println("Robot " + id + " no puede encontrar posición adyacente libre al origen.");
                        pedidoActual.marcarFallido();
                        finalizarPedido();
                        return false;
                    }
                } else {
                    Punto siguientePaso = calcularSiguientePaso(posicion, origen.getPosicion());
                    if (siguientePaso != null && esMovimientoValido(siguientePaso)) {
                        LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "mover_hacia_origen", posicion.toString(), siguientePaso.toString(), posicion.distanciaHacia(siguientePaso), bateriaActual + "/" + bateriaMaxima);
                        setPosicion(siguientePaso);
                        int consumoBateria = (int) Math.ceil(posicion.distanciaHacia(siguientePaso) * ParametrosGenerales.FACTOR_CONSUMO);
                        try {
                            consumirBateria(consumoBateria);
                            System.out.println("Robot " + id + " moviéndose hacia cofre origen. Posición: " + posicion + ", Batería: " + bateriaActual + "/" + bateriaMaxima);
                        } catch (IllegalStateException e) {
                            LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "fallo_bateria_mov_origen", posicion.toString(), siguientePaso.toString(), posicion.distanciaHacia(siguientePaso), bateriaActual + "/" + bateriaMaxima);
                            pedidoActual.marcarFallido();
                            finalizarPedido();
                            return false;
                        }
                    } else {
                        LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "bloqueo_mov_origen", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                        System.out.println("Robot " + id + " no puede moverse hacia el origen. Posición bloqueada.");
                        pedidoActual.marcarFallido();
                        finalizarPedido();
                        return false;
                    }
                }
                return true;
            }
            if (esAdyacente(posicion, origen.getPosicion()) && !cargaActual.containsKey(item)) {
                if (origen.getInventario().getCantidad(item) >= cantidad) {
                    try {
                        if (origen.removerItem(item, cantidad)) {
                            LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "cargar_item", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                            cargaActual.put(item, cantidad);
                            System.out.println("Robot " + id + " cargó " + cantidad + " unidades de " + item.getNombre() + " desde " + origen.getId());
                            consumirBateria(2);
                        } else {
                            LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "fallo_carga_item", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                            pedidoActual.marcarFallido();
                            finalizarPedido();
                            return false;
                        }
                    } catch (Exception e) {
                        LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "excepcion_carga_item", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                        pedidoActual.marcarFallido();
                        finalizarPedido();
                        return false;
                    }
                } else {
                    LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "sin_stock_origen", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                    System.out.println("Robot " + id + " no pudo cargar items. Cantidad insuficiente en " + origen.getId());
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            }
        }
        // 3. Moverse a una celda adyacente al cofre de destino
        if (cargaActual.containsKey(item) && !esAdyacente(posicion, destino.getPosicion())) {
            if (posicion.distanciaHacia(destino.getPosicion()) <= 1.5) {
                Punto posicionAdyacente = encontrarPosicionAdyacenteLibre(destino.getPosicion());
                if (posicionAdyacente != null) {
                    LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "mover_adjacente_destino", posicion.toString(), posicionAdyacente.toString(), posicion.distanciaHacia(posicionAdyacente), bateriaActual + "/" + bateriaMaxima);
                    setPosicion(posicionAdyacente);
                    System.out.println("Robot " + id + " llegó a una celda adyacente al cofre de destino " + destino.getId());
                    consumirBateria(2);
                } else {
                    LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "fallo_adjacente_destino", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                    System.out.println("Robot " + id + " no puede encontrar posición adyacente libre al destino.");
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            } else {
                Punto siguientePaso = calcularSiguientePaso(posicion, destino.getPosicion());
                if (siguientePaso != null && esMovimientoValido(siguientePaso)) {
                    LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "mover_hacia_destino", posicion.toString(), siguientePaso.toString(), posicion.distanciaHacia(siguientePaso), bateriaActual + "/" + bateriaMaxima);
                    setPosicion(siguientePaso);
                    int consumoBateria = (int) Math.ceil(posicion.distanciaHacia(siguientePaso) * ParametrosGenerales.FACTOR_CONSUMO * 1.5);
                    try {
                        consumirBateria(consumoBateria);
                        System.out.println("Robot " + id + " moviéndose hacia cofre destino. Posición: " + posicion + ", Batería: " + bateriaActual + "/" + bateriaMaxima);
                    } catch (IllegalStateException e) {
                        LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "fallo_bateria_mov_destino", posicion.toString(), siguientePaso.toString(), posicion.distanciaHacia(siguientePaso), bateriaActual + "/" + bateriaMaxima);
                        pedidoActual.marcarFallido();
                        finalizarPedido();
                        return false;
                    }
                } else {
                    LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "bloqueo_mov_destino", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                    System.out.println("Robot " + id + " no puede moverse hacia el destino. Posición bloqueada.");
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            }
            return true;
        }
        // 4. Entregar los items
        if (cargaActual.containsKey(item) && esAdyacente(posicion, destino.getPosicion())) {
            try {
                destino.agregarItem(item, cantidad);
                LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "descargar_item", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                    cargaActual.remove(item);
                    pedidoActual.marcarCompletado();
                    finalizarPedido();
                    return false;
            } catch (Exception e) {
                LoggerMovimientosRobots.getInstancia().logMovimiento(id, cicloActual, pedidoActual.toString(), "excepcion_descarga_item", posicion.toString(), posicion.toString(), 0, bateriaActual + "/" + bateriaMaxima);
                pedidoActual.marcarFallido();
                finalizarPedido();
                return false;
            }
        }
        return true;
    }

    /**
     * Calcula el siguiente paso hacia un destino, moviéndose solo 1 casillero.
     * Prioriza movimientos que mantengan al robot dentro del alcance de los robopuertos.
     * 
     * @param posicionActual Posición actual del robot
     * @param destino Posición de destino
     * @return Siguiente posición, o null si no se puede mover
     */
    private Punto calcularSiguientePaso(Punto posicionActual, Punto destino) {
        // Si ya estamos en el destino, no hay movimiento
        if (posicionActual.equals(destino)) {
            return null;
        }

        // Si estamos adyacentes al destino (robopuerto), permitir entrar directamente
        if (redLogistica != null) {
            boolean destinoEsRobopuerto = redLogistica.getRobopuertos().stream().anyMatch(rp -> rp.getPosicion().equals(destino));
            if (destinoEsRobopuerto && esAdyacente(posicionActual, destino)) {
                return destino;
            }
        }
        
        // Calcular dirección hacia el destino
        int dx = Integer.compare(destino.getX(), posicionActual.getX());
        int dy = Integer.compare(destino.getY(), posicionActual.getY());
        
        // Generar posibles movimientos
        List<Punto> movimientosPosibles = new ArrayList<>();
        
        // Movimiento horizontal
        if (dx != 0) {
            movimientosPosibles.add(new Punto(posicionActual.getX() + dx, posicionActual.getY()));
        }
        
        // Movimiento vertical
        if (dy != 0) {
            movimientosPosibles.add(new Punto(posicionActual.getX(), posicionActual.getY() + dy));
        }
        
        // Si no hay movimientos posibles, retornar null
        if (movimientosPosibles.isEmpty()) {
            return null;
        }
        
        // Filtrar movimientos válidos
        List<Punto> movimientosValidos = movimientosPosibles.stream()
                .filter(this::esMovimientoValido)
                .collect(Collectors.toList());
        
        // Si no hay movimientos válidos, retornar null
        if (movimientosValidos.isEmpty()) {
            System.out.println("Robot " + id + ": No hay movimientos válidos hacia " + destino);
            return null;
        }
        
        // Elegir el movimiento que más se acerque al destino
        Punto mejorMovimiento = movimientosValidos.stream()
                .min(Comparator.comparingDouble(p -> p.distanciaHacia(destino)))
                .orElse(movimientosValidos.get(0));
        
        System.out.println("Robot " + id + ": Movimiento seleccionado hacia " + mejorMovimiento + " (destino: " + destino + ")");
        return mejorMovimiento;
    }

    /**
     * Verifica si un movimiento es válido (no hay colisiones y está dentro del alcance).
     * 
     * @param nuevaPosicion Nueva posición a verificar
     * @param destinoFinal Destino final para validar la entrada a un robopuerto
     * @return true si el movimiento es válido, false en caso contrario
     */
    private boolean esMovimientoValido(Punto nuevaPosicion, Punto destinoFinal) {
        System.out.println("Robot " + id + ": Verificando movimiento a posición " + nuevaPosicion);
        
        // Verificar que no se solape con otros robots
        if (hayRobotEnPosicion(nuevaPosicion)) {
            System.out.println("Robot " + id + ": Movimiento inválido - colisión con robot");
            return false;
        }
        
        // Solo permitir entrar a un robopuerto si es el destino final
        boolean esRobopuerto = false;
        if (redLogistica != null) {
            esRobopuerto = redLogistica.getRobopuertos().stream().anyMatch(rp -> rp.getPosicion().equals(nuevaPosicion));
        }
        if (esRobopuerto && (destinoFinal == null || !nuevaPosicion.equals(destinoFinal))) {
            System.out.println("Robot " + id + ": Movimiento inválido - no puede atravesar robopuerto que no es destino final");
            return false;
        }
        
        // Verificar que no se solape con cofres
        if (hayCofreEnPosicion(nuevaPosicion)) {
            System.out.println("Robot " + id + ": Movimiento inválido - colisión con cofre");
            return false;
        }
        
        // Verificar que no se salga del alcance de un robopuerto
        if (!estaDentroDelAlcanceDeRobopuerto(nuevaPosicion)) {
            System.out.println("Robot " + id + ": Movimiento inválido - fuera del alcance de robopuertos");
            return false;
        }
        
        // Verificar que pueda regresar a un robopuerto desde la nueva posición
        if (!puedeRegresarARobopuerto(nuevaPosicion)) {
            System.out.println("Robot " + id + ": Movimiento inválido - no puede regresar a robopuerto desde " + nuevaPosicion);
            return false;
        }
        
        System.out.println("Robot " + id + ": Movimiento válido a posición " + nuevaPosicion);
        return true;
    }

    /**
     * Verifica si hay un robot en la posición especificada.
     * 
     * @param posicion Posición a verificar
     * @return true si hay un robot, false en caso contrario
     */
    private boolean hayRobotEnPosicion(Punto posicion) {
        if (redLogistica == null) {
            System.out.println("Robot " + id + ": No hay red logística para verificar colisiones con robots");
            return false; // Si no hay red, asumimos que no hay colisiones
        }
        
        boolean hayColision = redLogistica.getRobotsLogisticos().stream()
                .anyMatch(robot -> robot.getPosicion().equals(posicion) && !robot.equals(this));
        
        if (hayColision) {
            System.out.println("Robot " + id + ": Colisión detectada con otro robot en posición " + posicion);
        }
        
        return hayColision;
    }

    /**
     * Verifica si hay un robopuerto en la posición especificada.
     * 
     * @param posicion Posición a verificar
     * @return true si hay un robopuerto, false en caso contrario
     */
    private boolean hayRobopuertoEnPosicion(Punto posicion) {
        if (redLogistica == null) {
            System.out.println("Robot " + id + ": No hay red logística para verificar colisiones con robopuertos");
            return false; // Si no hay red, asumimos que no hay colisiones
        }
        
        boolean hayColision = redLogistica.getRobopuertos().stream()
                .anyMatch(robopuerto -> robopuerto.getPosicion().equals(posicion));
        
        if (hayColision) {
            System.out.println("Robot " + id + ": Colisión detectada con robopuerto en posición " + posicion);
        }
        
        return hayColision;
    }

    /**
     * Verifica si hay un cofre en la posición especificada.
     * 
     * @param posicion Posición a verificar
     * @return true si hay un cofre, false en caso contrario
     */
    private boolean hayCofreEnPosicion(Punto posicion) {
        if (redLogistica == null) {
            System.out.println("Robot " + id + ": No hay red logística para verificar colisiones con cofres");
            return false; // Si no hay red, asumimos que no hay colisiones
        }
        
        boolean hayColision = redLogistica.getCofres().stream()
                .anyMatch(cofre -> cofre.getPosicion().equals(posicion));
        
        if (hayColision) {
            System.out.println("Robot " + id + ": Colisión detectada con cofre en posición " + posicion);
        }
        
        return hayColision;
    }

    /**
     * Verifica si el robot necesita recargar.
     * 
     * @return true si necesita recargar, false en caso contrario
     */
    private boolean necesitaRecargar() {
        return bateriaActual < bateriaMaxima * 0.2; // Menos del 20% de batería
    }

    /**
     * Verifica si una posición está dentro del alcance de algún robopuerto.
     * 
     * @param posicion Posición a verificar
     * @return true si está dentro del alcance, false en caso contrario
     */
    private boolean estaDentroDelAlcanceDeRobopuerto(Punto posicion) {
        if (redLogistica == null) {
            System.out.println("Robot " + id + ": No hay red logística para verificar alcance de robopuertos");
            return true; // Si no hay red, asumimos que siempre está dentro del alcance
        }
        
        boolean dentroDelAlcance = redLogistica.getRobopuertos().stream()
                .anyMatch(robopuerto -> robopuerto.estaEnCobertura(posicion));
        
        if (!dentroDelAlcance) {
            System.out.println("Robot " + id + ": Posición " + posicion + " está fuera del alcance de todos los robopuertos");
        }
        
        return dentroDelAlcance;
    }

    /**
     * Verifica si el robot puede regresar a un robopuerto desde una posición dada.
     * Esto es importante para asegurar que el robot no se quede atrapado.
     * 
     * @param posicion Posición desde donde verificar
     * @return true si puede regresar, false en caso contrario
     */
    private boolean puedeRegresarARobopuerto(Punto posicion) {
        if (redLogistica == null) {
            return true; // Si no hay red, asumimos que puede regresar
        }
        
        // Buscar el robopuerto más cercano
        Robopuerto robopuertoMasCercano = redLogistica.getRobopuertoMasCercano(posicion);
        if (robopuertoMasCercano == null) {
            return false; // No hay robopuertos
        }
        
        // Verificar que esté dentro del alcance
        if (!robopuertoMasCercano.estaEnCobertura(posicion)) {
            return false;
        }
        
        // Verificar que tenga suficiente batería para llegar al robopuerto
        double distancia = posicion.distanciaHacia(robopuertoMasCercano.getPosicion());
        int bateriaNecesaria = (int) Math.ceil(distancia * ParametrosGenerales.FACTOR_CONSUMO);
        
        return tieneSuficienteBateria(bateriaNecesaria);
    }

    /**
     * Encuentra una posición adyacente libre a un punto dado.
     * 
     * @param punto Punto central
     * @return Posición adyacente libre, o null si no hay ninguna disponible
     */
    private Punto encontrarPosicionAdyacenteLibre(Punto punto) {
        int[][] direcciones = {{1,0}, {-1,0}, {0,1}, {0,-1}};
        
        for (int[] dir : direcciones) {
            Punto adyacente = new Punto(punto.getX() + dir[0], punto.getY() + dir[1]);
            if (esMovimientoValido(adyacente)) {
                System.out.println("Robot " + id + ": Encontrada posición adyacente libre en " + adyacente);
                return adyacente;
            }
        }
        
        System.out.println("Robot " + id + ": No se encontró ninguna posición adyacente libre a " + punto);
        return null; // No hay posición adyacente libre
    }

    public void cambiarEstado(EstadoRobot nuevoEstado) {
        Objects.requireNonNull(nuevoEstado, "El nuevo estado no puede ser nulo");

        if (!validarTransicion(this.estado, nuevoEstado)) {
            throw new IllegalStateException(
                    String.format("Transición inválida de %s a %s", this.estado, nuevoEstado));
        }

        this.estado = nuevoEstado;
        registrarCambioEstado(); //mostramos el cambio de estado en pantalla (Registrar los movimientos, distancias y decisiones tomadas en cada ciclo.)
    }

    private boolean validarTransicion(EstadoRobot actual, EstadoRobot nuevo) {
        if (actual == nuevo) return true; // si es el mismo que ya tiene, devolvemos true

        switch (actual) {
            case ACTIVO:
                return nuevo == EstadoRobot.PASIVO ||
                        nuevo == EstadoRobot.EN_MISION ||
                        nuevo == EstadoRobot.INACTIVO;

            case PASIVO:
                return nuevo == EstadoRobot.ACTIVO ||
                        nuevo == EstadoRobot.CARGANDO ||
                        nuevo == EstadoRobot.INACTIVO;

            case EN_MISION:
                return nuevo == EstadoRobot.ACTIVO ||
                        nuevo == EstadoRobot.PASIVO ||
                        nuevo == EstadoRobot.CARGANDO ||
                        nuevo == EstadoRobot.INACTIVO;

            case CARGANDO:
                return nuevo == EstadoRobot.ACTIVO ||
                        nuevo == EstadoRobot.PASIVO ||
                        nuevo == EstadoRobot.INACTIVO;

            case INACTIVO:
                return nuevo == EstadoRobot.ACTIVO; // Solo se puede reactivar

            default:
                return false;
        }
    }

    private void registrarCambioEstado() {
        System.out.println("com.alphaone.logisticaRobots.domain.Estado cambiado a: " + this.estado);
    }

    public boolean estaEnEstado(EstadoRobot estado) {
        return this.estado == estado; // Uso de == para comparaciones
    }

    // Métodos para manejar la batería

    private int validarBateria(int bateria) {
        if (bateria <= 0) {
            throw new IllegalArgumentException("La batería máxima debe ser un valor positivo");
        }
        return bateria;
    }

    public boolean tieneSuficienteBateria(int bateriaNecesaria) {
        return this.bateriaActual >= bateriaNecesaria;
    }

    public void consumirBateria(int cantidad) {
        if (!tieneSuficienteBateria(cantidad) || estado == EstadoRobot.INACTIVO) {
            throw new IllegalStateException("No hay suficiente batería o el robot está inactivo");
        }
        bateriaActual -= cantidad;
    }

    public void recargarBateria(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a recargar debe ser positiva");
        }
        if (this.bateriaActual + cantidad > this.bateriaMaxima) {
            throw new IllegalStateException("No se puede exceder la capacidad máxima de la batería");
        }
        this.bateriaActual += cantidad;
    }

    public boolean estadoValidoDeRecarga(EstadoRobot estado) {
        return estado == EstadoRobot.PASIVO || estado == EstadoRobot.CARGANDO;
    }

    //Métodos para manejar movimientos

    public void moverA(Punto nuevaUbicacion) {
        double distancia = posicion.distanciaHacia(nuevaUbicacion);
        int bateriaNecesaria = (int) Math.ceil(distancia * ParametrosGenerales.FACTOR_CONSUMO);  // Asumiendo factor de consumo = 1 | a cambiar esto para que sea dinámico

        if (!tieneSuficienteBateria(bateriaNecesaria)) {
            throw new IllegalStateException("Energía insuficiente para el movimiento");
        }

        consumirBateria(bateriaNecesaria);
        this.posicion = nuevaUbicacion;
    }

    // Métodos para manejar la carga (ítems)

    public boolean puedeCargar(int cantidad) {
        int cargaTotal = cargaActual.values().stream().mapToInt(Integer::intValue).sum();
        return cargaTotal + cantidad <= capacidadPedidosTraslado;
    }

    /**
     * Obtiene la ruta actual que está siguiendo el robot.
     * Calcula la ruta real que seguirá el robot basándose en su algoritmo de movimiento ortogonal,
     * desde la posición actual hasta su destino.
     * 
     * @return Lista de puntos que representan la ruta actual del robot
     */
    public List<Punto> getRutaActual() {
        List<Punto> ruta = new ArrayList<>();

        // Si no hay pedido actual pero hay pedidos pendientes, mostrar la ruta desde el último cofre de entrega al origen del siguiente pedido
        if (pedidoActual == null && !pedidosPendientes.isEmpty() && !historialPedidos.isEmpty() && redLogistica != null) {
            Pedido ultimoPedido = historialPedidos.get(historialPedidos.size() - 1);
            CofreLogistico cofreEntrega = ultimoPedido.getCofreDestino();
            Pedido siguientePedido = pedidosPendientes.peek();
            CofreLogistico cofreSiguienteOrigen = siguientePedido.getCofreOrigen();
            if (cofreEntrega != null && cofreSiguienteOrigen != null) {
                ruta = calcularRutaReal(cofreEntrega.getPosicion(), cofreSiguienteOrigen.getPosicion());
                // Si el origen es un cofre y la ruta termina en una celda adyacente, agregar el punto del cofre como último punto visual
                boolean destinoEsCofre = redLogistica.getCofres().stream().anyMatch(c -> c.getPosicion().equals(cofreSiguienteOrigen.getPosicion()));
                if (destinoEsCofre && !ruta.isEmpty()) {
                    Punto ultimo = ruta.get(ruta.size() - 1);
                    if (esAdyacente(ultimo, cofreSiguienteOrigen.getPosicion()) && !ultimo.equals(cofreSiguienteOrigen.getPosicion())) {
                        ruta.add(cofreSiguienteOrigen.getPosicion());
                    }
                }
            }
            return ruta;
        }

        // Si no hay pedido actual, no hay ruta
        if (pedidoActual == null) {
            return ruta;
        }

        // Obtener los cofres de origen y destino
        CofreLogistico origen = pedidoActual.getCofreOrigen();
        CofreLogistico destino = pedidoActual.getCofreDestino();
        Item item = pedidoActual.getItem();

        // Determinar el destino actual basado en el estado del pedido
        Punto puntoDestino;

        // Si no hemos recogido los items (no estamos en el origen o no tenemos el item), el destino es el origen
        if (!cargaActual.containsKey(item) || (!posicion.equals(origen.getPosicion()) && !cargaActual.containsKey(item))) {
            puntoDestino = origen.getPosicion();
        }
        // Si ya recogimos los items y no estamos en el destino, el destino es el destino
        else if (cargaActual.containsKey(item) && !posicion.equals(destino.getPosicion())) {
            puntoDestino = destino.getPosicion();
        }
        // Si ya estamos en el destino, no hay ruta
        else {
            return ruta;
        }

        // Si el robot está en estado PASIVO o CARGANDO, mostrar solo la ruta al robopuerto más cercano
        if (estado == EstadoRobot.PASIVO || estado == EstadoRobot.CARGANDO) {
            if (redLogistica != null) {
                Robopuerto robopuertoCercano = redLogistica.getRobopuertoMasCercano(posicion);
                if (robopuertoCercano != null) {
                    ruta = calcularRutaReal(posicion, robopuertoCercano.getPosicion());
                }
            }
            return ruta;
        }

        // Calcular el consumo de batería necesario para llegar al destino
        double consumoEstimado = posicion.distanciaHacia(puntoDestino) * ParametrosGenerales.FACTOR_CONSUMO;
        if (!tieneSuficienteBateria((int)Math.ceil(consumoEstimado))) {
            // No hay suficiente batería, planificar desvío a robopuerto más cercano
            if (redLogistica != null) {
                Robopuerto robopuertoCercano = redLogistica.getRobopuertoMasCercano(posicion);
                if (robopuertoCercano != null) {
                    // Ruta hasta el robopuerto
                    List<Punto> rutaARobopuerto = calcularRutaReal(posicion, robopuertoCercano.getPosicion());
                    // Ruta desde el robopuerto al destino
                    List<Punto> rutaDesdeRobopuerto = calcularRutaReal(robopuertoCercano.getPosicion(), puntoDestino);
                    // Unir ambas rutas (evitar duplicar el punto de robopuerto)
                    ruta.addAll(rutaARobopuerto);
                    if (!rutaDesdeRobopuerto.isEmpty()) {
                        if (!rutaARobopuerto.isEmpty() && rutaARobopuerto.get(rutaARobopuerto.size()-1).equals(rutaDesdeRobopuerto.get(0))) {
                            rutaDesdeRobopuerto.remove(0);
                        }
                        ruta.addAll(rutaDesdeRobopuerto);
                    }
                    return ruta;
                }
            }
        }

        // Calcular la ruta real que seguirá el robot usando su algoritmo de movimiento
        ruta = calcularRutaReal(posicion, puntoDestino);

        // Si el destino es un cofre y la ruta termina en una celda adyacente, agregar el punto del cofre como último punto visual
        boolean destinoEsCofre = false;
        if (puntoDestino != null && redLogistica != null) {
            destinoEsCofre = redLogistica.getCofres().stream().anyMatch(c -> c.getPosicion().equals(puntoDestino));
        }
        if (destinoEsCofre && !ruta.isEmpty()) {
            Punto ultimo = ruta.get(ruta.size() - 1);
            if (esAdyacente(ultimo, puntoDestino) && !ultimo.equals(puntoDestino)) {
                ruta.add(puntoDestino);
            }
        }

        return ruta;
    }

    /**
     * Calcula la ruta real que seguirá el robot desde su posición actual hasta el destino,
     * usando el mismo algoritmo de movimiento que usa para moverse.
     * 
     * @param posicionActual Posición actual del robot
     * @param destino Posición de destino
     * @return Lista de puntos que representan la ruta real
     */
    private List<Punto> calcularRutaReal(Punto posicionActual, Punto destino) {
        List<Punto> ruta = new ArrayList<>();
        Punto posicionSimulada = new Punto(posicionActual.getX(), posicionActual.getY());
        
        // Agregar la posición actual como primer punto
        ruta.add(new Punto(posicionSimulada.getX(), posicionSimulada.getY()));
        
        // Simular el movimiento paso a paso hasta llegar al destino
        int maxPasos = 100; // Límite para evitar bucles infinitos
        int pasos = 0;
        
        while (!posicionSimulada.equals(destino) && pasos < maxPasos) {
            // Usar el mismo algoritmo que usa el robot para calcular el siguiente paso, pero validando obstáculos
            Punto siguientePaso = calcularSiguientePasoSimuladoConObstaculos(posicionSimulada, destino);
            
            if (siguientePaso == null) {
                // No se puede avanzar más, terminar la ruta
                break;
            }
            
            // Agregar el siguiente paso a la ruta
            ruta.add(new Punto(siguientePaso.getX(), siguientePaso.getY()));
            posicionSimulada = siguientePaso;
            pasos++;
        }
        
        return ruta;
    }

    /**
     * Versión simulada del cálculo de siguiente paso para generar la ruta.
     * Verifica obstáculos igual que el movimiento real (cofres, robots, robopuertos, etc.).
     * No consume batería ni cambia el estado real del robot.
     * 
     * @param posicionActual Posición actual
     * @param destino Posición de destino
     * @return Siguiente posición en la ruta
     */
    private Punto calcularSiguientePasoSimuladoConObstaculos(Punto posicionActual, Punto destino) {
        // Si ya estamos en el destino, no hay movimiento
        if (posicionActual.equals(destino)) {
            return null;
        }
        
        // Calcular dirección hacia el destino
        int dx = Integer.compare(destino.getX(), posicionActual.getX());
        int dy = Integer.compare(destino.getY(), posicionActual.getY());
        
        // Generar posibles movimientos ortogonales
        List<Punto> movimientosPosibles = new ArrayList<>();
        
        // Movimiento horizontal
        if (dx != 0) {
            movimientosPosibles.add(new Punto(posicionActual.getX() + dx, posicionActual.getY()));
        }
        
        // Movimiento vertical
        if (dy != 0) {
            movimientosPosibles.add(new Punto(posicionActual.getX(), posicionActual.getY() + dy));
        }
        
        // Filtrar movimientos válidos (sin colisiones ni obstáculos)
        List<Punto> movimientosValidos = movimientosPosibles.stream()
                .filter(this::esMovimientoValido)
                .collect(Collectors.toList());
        
        if (movimientosValidos.isEmpty()) {
            return null;
        }
        
        // Elegir el movimiento que más se acerque al destino
        Punto mejorMovimiento = movimientosValidos.stream()
                .min(Comparator.comparingDouble(p -> p.distanciaHacia(destino)))
                .orElse(movimientosValidos.get(0));
        
        return mejorMovimiento;
    }

    // Sobrecarga para mantener compatibilidad con otras llamadas
    private boolean esMovimientoValido(Punto nuevaPosicion) {
        return esMovimientoValido(nuevaPosicion, null);
    }

}
