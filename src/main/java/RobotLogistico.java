import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;
import com.alphaone.logisticaRobots.domain.Punto;

import java.util.*;

public class RobotLogistico /*implements Ubicable*/ {
    private final int id;
    private Punto posicion;
    private final int bateriaMaxima;
    private int bateriaActual;  // Cambiado a entero simple
    private final int capacidadPedidosTraslado;
    private EstadoRobot estado;
    private Map<Item, Integer> cargaActual;  // Los ítems que está transportando
    private final Robopuerto robopuertoBase; // el robot empieza en un nodo del camino y puede desplazarse a otro en base a los nodos, sus pesos y cantidad de robots que se tenga
    private Queue<Pedido> pedidosEncolados; // debe tener una cola de pedidos

    public RobotLogistico(int id, Punto posicion, Robopuerto robopuertoBase, int bateriaMaxima, int capacidadPedidosTraslado) {
        this.id = id;
        this.posicion = Objects.requireNonNull(posicion, "Posición no puede ser null");
        this.bateriaMaxima = validarBateria(bateriaMaxima);
        this.bateriaActual = this.bateriaMaxima;  // Inicia con la batería llena
        this.capacidadPedidosTraslado = validarCapacidadDeTraslado(capacidadPedidosTraslado);
        this.estado = EstadoRobot.ACTIVO; // Estado inicial
        this.cargaActual = new HashMap<>();  // Debemos inicializarlo con la carga máxima. PENDIENTE (Deberíamos hacer un inventarios también?
        this.robopuertoBase = Objects.requireNonNull(robopuertoBase); // de algún lado tiene que salir
        this.pedidosEncolados = new LinkedList<>();
    }

    //Getters:

    public int getBateriaActual() {return bateriaActual;}
    public int getBateriaMaxima() {return bateriaMaxima;}
    public Punto getPosicion() {return posicion;}
    public EstadoRobot getEstado() {return estado;}

    // Métodos para manejar los estados del robot

    /*
    public void iniciarMision() {
        cambiarEstado(EstadoRobot.EN_MISION);
    }

    public void ponerEnEspera() {
        cambiarEstado(EstadoRobot.PASIVO);
    }

    public void iniciarRecarga() {
        cambiarEstado(EstadoRobot.CARGANDO);
    }

    public void activar() {
        cambiarEstado(EstadoRobot.ACTIVO);
    }

    public void desactivar() {
        cambiarEstado(EstadoRobot.INACTIVO);
    }
    esto tal vez se saque ya que para eso hice la clase "cambiarEstado"*/

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
        System.out.println("Estado cambiado a: " + this.estado);
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

    public void recargarBateria(int cantidad) { //la bateria la recarga el robopuerto, pero para manejo de bateria en la propia clase, se hace acá
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

    private int validarCapacidadDeTraslado(int capacidad) {
        if (capacidad <= 0) {
            throw new IllegalArgumentException("La capacidad debe ser un valor positivo");
        }
        return capacidad;
    }

    public boolean puedeCargar(int cantidad) {
        int cargaTotal = cargaActual.values().stream().mapToInt(Integer::intValue).sum();
        return cargaTotal + cantidad <= capacidadPedidosTraslado;
    }


    private void cargarDesdeCofre(CofreLogistico cofre) {

        if(this.pedidosEncolados.isEmpty() || cofre == null) { throw new IllegalArgumentException("No se puede cargar desde Cofre"); }

        Map<Item, Integer> items = cofre.obtenerItemsDisponibles();

        //por cada pedido tomado, tengo que preguntar si dentro de ese pedido consultado, se encuentra el cofre del cual obtener productos y empezar a cargar "CargaActual"

        /*for(pedidosEncolados ){

            if(pedidosEncolados.getCofre)
            //por cada pedido encolado
            for (Map.Entry<Item, Integer> entry : items.entrySet()) {
                int cantidadDisponible = entry.getValue();
                int capacidadRestante = capacidadPedidosTraslado - cargaActual.values().stream().mapToInt(Integer::intValue).sum(); //acá deberíamos tener lo mismo que inventarios, no?
                int cantidadATomar = Math.min(cantidadDisponible, capacidadRestante);

                if (cantidadATomar > 0) {
                    cofre.retirarItems(entry.getKey(), cantidadATomar);
                    cargaActual.merge(entry.getKey(), cantidadATomar, Integer::sum);
                }
            }
        }*/
    }

    private void descargarEnCofre(CofreLogistico cofre) {
//        for (Map.Entry<Item, Integer> entry : cargaActual.entrySet()) {
//            int cantidadRequerida = cofre.getCantidadRequerida(entry.getKey());
//            int cantidadADescargar = Math.min(entry.getValue(), cantidadRequerida);
//
//            if (cantidadADescargar > 0) {
//                cofre.recibirItems(entry.getKey(), cantidadADescargar);
//                cargaActual.merge(entry.getKey(), -cantidadADescargar, Integer::sum);
//            }
//        }
//        cargaActual.values().removeIf(val -> val <= 0);
    }

}
