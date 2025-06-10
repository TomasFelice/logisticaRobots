package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.Punto;


import java.util.*;

public class RobotLogistico implements Ubicable {
    private final int id;
    private Punto posicion;
    private final int bateriaMaxima;
    private int bateriaActual;  // Cambiado a entero simple
    private final static int capacidadPedidosTraslado = 10; // Los robots transportan una cantidad predefinida de ítems en cada viaje. Esta cantidad debe ser configurable a nivel global al momento de correr la simulación.
    private EstadoRobot estado;
    private Map<Item, Integer> cargaActual;  // Los ítems que está transportando y su cantidad
    private final Robopuerto robopuertoBase; // el robot empieza en un nodo del camino y puede desplazarse a otro en base a los nodos, sus pesos y cantidad de robots que se tenga
    private Queue<Pedido> pedidosEncolados; // debe tener una cola de pedidos [TODO] Cambiar por cola de prioridad
    private final Queue<Pedido> pedidosPendientes = new LinkedList<>();
    private final List<Pedido> historialPedidos = new ArrayList<>();

    private Pedido pedidoActual;

    public RobotLogistico(int id, Punto posicion, Robopuerto robopuertoBase, int bateriaMaxima, int capacidadPedidosTraslado) {
        this.id = id;
        this.posicion = Objects.requireNonNull(posicion, "Posición no puede ser null");
        this.bateriaMaxima = validarBateria(bateriaMaxima);
        this.bateriaActual = this.bateriaMaxima;  // Inicia con la batería llena
        this.estado = EstadoRobot.ACTIVO; // com.alphaone.logisticaRobots.domain.Estado inicial
        this.cargaActual = new HashMap<>();  // Debemos inicializarlo con la carga máxima. PENDIENTE (Deberíamos hacer un inventarios también?
        this.robopuertoBase = Objects.requireNonNull(robopuertoBase); // de algún lado tiene que empezar el robot
        this.pedidosEncolados = new LinkedList<>();
    }

    //Getters:

    public int getBateriaActual() {return bateriaActual;}
    public int getBateriaMaxima() {return bateriaMaxima;}

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

    // Métodos para manejar los estados del robot

    /*
    public void iniciarMision() {
        cambiarEstado(com.alphaone.logisticaRobots.domain.EstadoRobot.EN_MISION);
    }

    public void ponerEnEspera() {
        cambiarEstado(com.alphaone.logisticaRobots.domain.EstadoRobot.PASIVO);
    }

    public void iniciarRecarga() {
        cambiarEstado(com.alphaone.logisticaRobots.domain.EstadoRobot.CARGANDO);
    }

    public void activar() {
        cambiarEstado(com.alphaone.logisticaRobots.domain.EstadoRobot.ACTIVO);
    }

    public void desactivar() {
        cambiarEstado(com.alphaone.logisticaRobots.domain.EstadoRobot.INACTIVO);
    }
    esto tal vez se saque ya que para eso hice la clase "cambiarEstado"*/
    public void agregarPedido(Pedido pedido) {
        if (pedido == null) throw new IllegalArgumentException("El pedido no puede ser null");
        pedidosPendientes.add(pedido);
    }

    private void finalizarPedido() {
        historialPedidos.add(pedidoActual);
        pedidoActual = null;
        cambiarEstado(EstadoRobot.ACTIVO);
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
        if (tieneSuficienteBateria(cantidad) && estado != EstadoRobot.INACTIVO) {
            throw new IllegalStateException("No hay suficiente batería");
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
        int bateriaNecesaria = (int) Math.ceil(distancia);  // Asumiendo factor de consumo = 1 | a cambiar esto para que sea dinámico

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


    public void cargarDesdeCofre(CofreLogistico cofre) {

        Inventario inventarioCofre = cofre.getInventario();

        if (inventarioCofre.estaVacio()) {
            throw new IllegalArgumentException("El cofre está vacío, no hay items para cargar");
        }

        int totalDisponibleEnCofre = inventarioCofre.getTotalItems();

        if (!puedeCargar(totalDisponibleEnCofre)) {
            throw new IllegalArgumentException(
                    String.format("El robot no puede cargar %d unidades (capacidad: %d)",
                            totalDisponibleEnCofre, capacidadPedidosTraslado)
            );
        }

        // Si pasó todas las validaciones, proceder a transferir todos los items
        Map<Item, Integer> itemsATransferir = new HashMap<>(inventarioCofre.getTodos());

        for (Map.Entry<Item, Integer> entry : itemsATransferir.entrySet()) {
            Item item = entry.getKey();
            int cantidad = entry.getValue();

            if (cantidad > 0 && inventarioCofre.remover(item, cantidad)) {
                cargaActual.merge(item, cantidad, Integer::sum);
            }
        }
    }

    public void descargarACofre(CofreLogistico cofre) {

        if (cargaActual == null || cargaActual.isEmpty()) {
            throw new IllegalArgumentException("El robot no tiene carga para descargar");
        }

        Inventario inventarioCofre = cofre.getInventario();

        for (Map.Entry<Item, Integer> entry : new HashMap<>(cargaActual).entrySet()) {
            Item item = entry.getKey();
            int cantidad = entry.getValue();

            if (cantidad > 0) {
                inventarioCofre.agregar(item, cantidad);
                cargaActual.remove(item);
            }
        }
    }


}
