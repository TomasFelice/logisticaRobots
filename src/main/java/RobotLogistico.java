import com.alphaone.logisticaRobots.domain.Punto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

public class RobotLogistico /*implements Ubicable*/ {
    private final int id;
    private Punto posicion;
    private final Robopuerto robopuertoBase; // esto a validar
    private final int bateriaMaxima;
    private int bateriaActual;  // Cambiado a entero simple
    private final int capacidadPedidosTraslado;
    private EstadoRobot estado;
    private Map<Item, Integer> cargaActual;  // Los ítems que está transportando

    public RobotLogistico(int id, Coordenada ubicacion, Robopuerto robopuertoBase, int bateriaMaxima, int capacidadPedidosTraslado) {
        this.id = id;
        this.posicion = requireNonNull(posicion, "Posición no puede ser null");
        this.robopuertoBase = requireNonNull(robopuertoBase); // de algún lado tiene que salir
        this.bateriaMaxima = validarBateria(bateriaMaxima);
        this.bateriaActual = this.bateriaMaxima;  // Inicia con la batería llena
        this.capacidadPedidosTraslado = validarCapacidadDeTraslado(capacidadPedidosTraslado);
        this.estado = EstadoRobot.ACTIVO; // Estado inicial
        this.cargaActual = new HashMap<>();  // Debemos inicializarlo con la carga máxima. PENDIENTE
    }

    public int getBateriaActual() {return bateriaActual;}
    public int getBateriaMaxima() {return bateriaMaxima;}
    public Punto getPosicion() {return posicion;}
    public EstadoRobot getEstado() {return estado;}

    // Métodos para manejar los estados del robot

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

    public void cambiarEstado(EstadoRobot nuevoEstado) {
        requireNonNull(nuevoEstado, "El nuevo estado no puede ser nulo");

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
        if (cantidad > bateriaActual && estado != EstadoRobot.INACTIVO) {
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

    public void agregarCarga(Item item, int cantidad) {
        if (!puedeCargar(cantidad)) {
            throw new IllegalStateException("Capacidad de carga excedida");
        }
        cargaActual.merge(item, cantidad, Integer::sum);
    }

    public void descargarCarga(Item item, int cantidad) { //para despachar en Cofres - pendiente
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser positiva");
        }
        Integer cantidadActual = cargaActual.get(item);
        if (cantidadActual == null || cantidadActual < cantidad) {
            throw new IllegalStateException("No hay suficiente cantidad del ítem para descargar");
        }
        if(cantidadActual == cantidad){
            cargaActual.remove(item);
        }else{
            cargaActual.put(item, cantidadActual - cantidad);
        }
    }
}
