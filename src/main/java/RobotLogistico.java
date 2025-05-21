import java.util.HashMap;
import java.util.Map;

public class RobotLogistico implements Ubicable {
    private final int id;
    private Coordenada ubicacion;
    private final int bateriaMaxima;
    private int bateriaActual;  // Cambiado a entero simple
    private final int capacidadPedidosTraslado;
    private Estado estado;
    private Map<Item, Integer> cargaActual;  // Los ítems que está transportando

    public RobotLogistico(int id, Coordenada ubicacion, int bateriaMaxima, int capacidadPedidosTraslado) {
        this.id = id;
        this.ubicacion = ubicacion;
        this.bateriaMaxima = bateriaMaxima;
        this.bateriaActual = bateriaMaxima;  // Inicia con la batería llena
        this.capacidadPedidosTraslado = capacidadPedidosTraslado;
        this.estado = new EstadoRobot("Activo"); // Estado inicial
        this.cargaActual = new HashMap<>();  // Debemos inicializarlo con la carga máxima. PENDIENTE
    }
    // Métodos para manejar la batería
    public boolean tieneSuficienteBateria(int bateriaNecesaria) {
        return this.bateriaActual >= bateriaNecesaria;
    }

    public void consumirBateria(int cantidad) {
        if (cantidad > bateriaActual) {
            throw new IllegalStateException("No hay suficiente energía");
        }
        bateriaActual -= cantidad;
    }

    public void recargarBateria() {
        bateriaActual = bateriaMaxima;
        // hay que implementar acá el tema del cambio de estado
    }

    public int getBateriaActual() {
        return bateriaActual;
    }

    // Implementación de Ubicable
    @Override
    public Coordenada obtenerUbicacion() {
        return ubicacion;
    }

    @Override
    public void moverA(Coordenada nuevaUbicacion) {
        double distancia = ubicacion.distanciaEuclidea(nuevaUbicacion);
        int bateriaNecesaria = (int) Math.ceil(distancia);  // Asumiendo factor de consumo = 1

        if (!tieneSuficienteBateria(bateriaNecesaria)) {
            throw new IllegalStateException("Energía insuficiente para el movimiento");
        }

        consumirBateria(bateriaNecesaria);
        this.ubicacion = nuevaUbicacion;
    }

    // Métodos para manejar la carga (ítems)
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
}
