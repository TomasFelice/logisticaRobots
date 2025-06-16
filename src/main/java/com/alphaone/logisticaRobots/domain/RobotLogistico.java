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
    public int getId() {return id;}
    public Robopuerto getRobopuertoBase() {return robopuertoBase;}
    public Map<Item, Integer> getCargaActual() {return cargaActual;}
    public int getCantidadPedidosEncolados() {return pedidosEncolados.size();}
    public int getCantidadPedidosPendientes() {return pedidosPendientes.size();}
    public int getCantidadHistorialPedidos() {return historialPedidos.size();}
    public int getCantidadPedidos() {return historialPedidos.size() + pedidosPendientes.size();}
    public int getCantidadPedidosEnProceso() {return (pedidoActual != null) ? 1 : 0;}
    public int getCantidadPedidosCompletados() {return historialPedidos.size();}

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

    /**
     * Procesa el siguiente pedido pendiente.
     * Este método debe ser llamado en cada ciclo de simulación para que el robot
     * avance en la ejecución de sus pedidos.
     * 
     * @return true si se procesó un pedido, false si no había pedidos pendientes
     */
    public boolean procesarSiguientePedido() {
        // Si ya hay un pedido en proceso, continuar con él
        if (pedidoActual != null) {
            return continuarPedidoActual();
        }

        // Si no hay pedido actual pero hay pendientes, tomar el siguiente
        if (!pedidosPendientes.isEmpty()) {
            pedidoActual = pedidosPendientes.poll();
            pedidoActual.marcarEnProceso();
            cambiarEstado(EstadoRobot.EN_MISION);
            return true;
        }

        // No hay pedidos para procesar
        return false;
    }

    /**
     * Continúa con el procesamiento del pedido actual.
     * 
     * @return true si el pedido sigue en proceso, false si se completó o falló
     */
    private boolean continuarPedidoActual() {
        if (pedidoActual == null) {
            return false;
        }

        // Verificar si tenemos suficiente batería para continuar
        if (!tieneSuficienteBateria(10)) {
            // Necesitamos recargar
            cambiarEstado(EstadoRobot.PASIVO);
            System.out.println("Robot " + id + " necesita recargar. Batería actual: " + bateriaActual);
            return true;
        }

        // Obtener los cofres de origen y destino
        CofreLogistico origen = pedidoActual.getCofreOrigen();
        CofreLogistico destino = pedidoActual.getCofreDestino();
        Item item = pedidoActual.getItem();
        int cantidad = pedidoActual.getCantidad();

        // Estado del pedido: 
        // 1. Moverse al cofre de origen
        // 2. Recoger los items
        // 3. Moverse al cofre de destino
        // 4. Entregar los items

        // Verificar si estamos en el cofre de origen
        if (!posicion.equals(origen.getPosicion())) {
            // Moverse hacia el cofre de origen
            double distancia = posicion.distanciaHacia(origen.getPosicion());

            // Si estamos muy cerca, llegamos directamente
            if (distancia <= 1.0) {
                setPosicion(origen.getPosicion());
                System.out.println("Robot " + id + " llegó al cofre de origen " + origen.getId());
            } else {
                // Calcular cuánto nos movemos en este ciclo (máximo 5 unidades)
                double movimiento = Math.min(5.0, distancia);

                // Calcular nueva posición (movimiento parcial hacia el destino)
                double porcentajeMovimiento = movimiento / distancia;
                int newX = (int) (posicion.getX() + (origen.getPosicion().getX() - posicion.getX()) * porcentajeMovimiento);
                int newY = (int) (posicion.getY() + (origen.getPosicion().getY() - posicion.getY()) * porcentajeMovimiento);

                setPosicion(new Punto(newX, newY));

                // Consumir batería basado en la distancia recorrida
                int consumoBateria = (int) Math.ceil(movimiento);
                try {
                    consumirBateria(consumoBateria);
                    System.out.println("Robot " + id + " moviéndose hacia cofre origen. Posición: " + posicion + 
                                      ", Batería: " + bateriaActual + "/" + bateriaMaxima);
                } catch (IllegalStateException e) {
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            }
            return true;
        }

        // Si estamos en el cofre de origen pero no hemos cargado los items
        if (posicion.equals(origen.getPosicion()) && !cargaActual.containsKey(item)) {
            // Verificar si el cofre tiene suficientes items
            if (origen.getInventario().getCantidad(item) >= cantidad) {
                try {
                    // Cargar el item desde el cofre
                    if (origen.removerItem(item, cantidad)) {
                        cargaActual.put(item, cantidad);
                        System.out.println("Robot " + id + " cargó " + cantidad + " unidades de " + 
                                          item.getNombre() + " desde " + origen.getId());

                        // Consumir batería por la operación de carga
                        consumirBateria(2);
                    } else {
                        // No se pudo remover el item (quizás otro robot lo tomó mientras tanto)
                        pedidoActual.marcarFallido();
                        finalizarPedido();
                        return false;
                    }
                } catch (Exception e) {
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            } else {
                // No hay suficientes items
                System.out.println("Robot " + id + " no pudo cargar items. Cantidad insuficiente en " + origen.getId());
                pedidoActual.marcarFallido();
                finalizarPedido();
                return false;
            }
        }

        // Si ya cargamos los items pero no estamos en el destino
        if (cargaActual.containsKey(item) && !posicion.equals(destino.getPosicion())) {
            // Moverse hacia el cofre de destino
            double distancia = posicion.distanciaHacia(destino.getPosicion());

            // Si estamos muy cerca, llegamos directamente
            if (distancia <= 1.0) {
                setPosicion(destino.getPosicion());
                System.out.println("Robot " + id + " llegó al cofre de destino " + destino.getId());
            } else {
                // Calcular cuánto nos movemos en este ciclo (máximo 5 unidades)
                double movimiento = Math.min(5.0, distancia);

                // Calcular nueva posición (movimiento parcial hacia el destino)
                double porcentajeMovimiento = movimiento / distancia;
                int newX = (int) (posicion.getX() + (destino.getPosicion().getX() - posicion.getX()) * porcentajeMovimiento);
                int newY = (int) (posicion.getY() + (destino.getPosicion().getY() - posicion.getY()) * porcentajeMovimiento);

                setPosicion(new Punto(newX, newY));

                // Consumir batería basado en la distancia recorrida (más consumo por llevar carga)
                int consumoBateria = (int) Math.ceil(movimiento * 1.5); // 50% más de consumo por llevar carga
                try {
                    consumirBateria(consumoBateria);
                    System.out.println("Robot " + id + " moviéndose hacia cofre destino. Posición: " + posicion + 
                                      ", Batería: " + bateriaActual + "/" + bateriaMaxima);
                } catch (IllegalStateException e) {
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            }
            return true;
        }

        // Si estamos en el destino y tenemos los items, entregarlos
        if (posicion.equals(destino.getPosicion()) && cargaActual.containsKey(item)) {
            try {
                // Entregar los items al cofre destino
                int cantidadEntrega = cargaActual.get(item);
                if (destino.agregarItem(item, cantidadEntrega)) {
                    // Limpiar la carga del robot
                    cargaActual.remove(item);

                    // Marcar el pedido como completado
                    pedidoActual.marcarCompletado();
                    System.out.println("Robot " + id + " entregó " + cantidadEntrega + " unidades de " + 
                                      item.getNombre() + " a " + destino.getId() + ". Pedido completado.");

                    // Consumir batería por la operación de descarga
                    consumirBateria(2);

                    finalizarPedido();
                    return false;
                } else {
                    // No se pudo entregar (quizás el cofre está lleno)
                    System.out.println("Robot " + id + " no pudo entregar items. Cofre " + destino.getId() + " sin espacio.");
                    pedidoActual.marcarFallido();
                    finalizarPedido();
                    return false;
                }
            } catch (Exception e) {
                pedidoActual.marcarFallido();
                finalizarPedido();
                return false;
            }
        }

        // Si llegamos aquí, algo salió mal
        System.out.println("Robot " + id + " encontró un estado inesperado en el procesamiento del pedido.");
        pedidoActual.marcarFallido();
        finalizarPedido();
        return false;
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

    /**
     * Obtiene la ruta actual que está siguiendo el robot.
     * Calcula una ruta desde la posición actual del robot hasta su destino,
     * basándose en el estado actual del pedido que está procesando.
     * 
     * @return Lista de puntos que representan la ruta actual del robot
     */
    public List<Punto> getRutaActual() {
        List<Punto> ruta = new ArrayList<>();

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

        // Si no hemos llegado al origen o no hemos cargado los items, el destino es el origen
        if (!posicion.equals(origen.getPosicion()) || !cargaActual.containsKey(item)) {
            puntoDestino = origen.getPosicion();
        } 
        // Si ya cargamos los items pero no estamos en el destino, el destino es el destino
        else if (cargaActual.containsKey(item) && !posicion.equals(destino.getPosicion())) {
            puntoDestino = destino.getPosicion();
        } 
        // Si ya estamos en el destino, no hay ruta
        else {
            return ruta;
        }

        // Calcular la ruta como una línea recta entre la posición actual y el destino
        // Dividimos la distancia en segmentos para crear una ruta más detallada
        double distancia = posicion.distanciaHacia(puntoDestino);
        int numSegmentos = Math.max(10, (int)distancia / 5); // Al menos 10 segmentos, o uno cada 5 unidades

        // Agregar la posición actual como primer punto de la ruta
        ruta.add(new Punto(posicion.getX(), posicion.getY()));

        // Calcular puntos intermedios
        for (int i = 1; i <= numSegmentos; i++) {
            double porcentaje = (double) i / numSegmentos;
            int x = (int) (posicion.getX() + (puntoDestino.getX() - posicion.getX()) * porcentaje);
            int y = (int) (posicion.getY() + (puntoDestino.getY() - posicion.getY()) * porcentaje);
            ruta.add(new Punto(x, y));
        }

        return ruta;
    }


}
