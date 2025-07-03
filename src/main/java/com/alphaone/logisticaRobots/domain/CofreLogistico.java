package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.comportamiento.ComportamientoCofre;
import com.alphaone.logisticaRobots.domain.pathfinding.Punto;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Representa un cofre logístico con diferentes comportamientos por item
 */
public class CofreLogistico implements Ubicable {
    private final String id;
    private final Punto posicion;
    private final int capacidadMaxima;
    private final Inventario inventario;
    private final Map<Item, ComportamientoCofre> comportamientos;
    private ComportamientoCofre comportamientoPorDefecto;

    public CofreLogistico(String id, Punto posicion, int capacidadMaxima) {
        this.id = Objects.requireNonNull(id, "ID no puede ser null");
        this.posicion = Objects.requireNonNull(posicion, "Posición no puede ser null");
        this.capacidadMaxima = capacidadMaxima;
        this.inventario = new Inventario();
        this.comportamientos = new HashMap<>();

        if (capacidadMaxima <= 0) {
            throw new IllegalArgumentException("Capacidad máxima debe ser positiva");
        }
    }

    public CofreLogistico(Punto posicion, int capacidadMaxima) {
        this(UUID.randomUUID().toString(), posicion, capacidadMaxima);
    }

    // Getters
    public String getId() { return id; }

    @Override
    public Punto getPosicion() { return posicion; }

    @Override
    public void setPosicion(Punto posicion) {
        // Cannot set position for CofreLogistico as it's a final field
        throw new UnsupportedOperationException("No se puede cambiar la posición de un cofre logístico");
    }

    public int getCapacidadMaxima() { return capacidadMaxima; }
    public Inventario getInventario() { return inventario; }

    // Configuración de comportamientos
    public void setComportamientoPorDefecto(ComportamientoCofre comportamiento) {
        this.comportamientoPorDefecto = comportamiento;
    }

    public void setComportamiento(Item item, ComportamientoCofre comportamiento) {
        comportamientos.put(item, comportamiento);
    }

    private ComportamientoCofre getComportamiento(Item item) {
        return comportamientos.getOrDefault(item, comportamientoPorDefecto);
    }

    // Operaciones de inventario
    public boolean agregarItem(Item item, int cantidad) {
        if (inventario.getTotalItems() + cantidad > capacidadMaxima) {
            return false;
        }

        inventario.agregar(item, cantidad);
        return true;
    }

    public boolean removerItem(Item item, int cantidad) {
        return inventario.remover(item, cantidad);
    }

    // Funciones de comportamiento
    public boolean puedeOfrecer(Item item, int cantidad) {
        ComportamientoCofre comportamiento = getComportamiento(item);
        return comportamiento != null && comportamiento.puedeOfrecer(item, cantidad, this);
    }

    public boolean puedeSolicitar(Item item, int cantidad) {
        ComportamientoCofre comportamiento = getComportamiento(item);
        return comportamiento != null && comportamiento.puedeSolicitar(item, cantidad, this);
    }

    public String getTipoComportamiento(Item item) {
        ComportamientoCofre comportamiento = getComportamiento(item);
        return comportamiento != null ? comportamiento.getTipo() : "Sin comportamiento";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CofreLogistico that = (CofreLogistico) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("CofreLogistico{id='%s', posicion=%s, capacidad=%d/%d}",
                id, posicion, inventario.getTotalItems(), capacidadMaxima);
    }

    public Map<Item, Integer> obtenerItemsDisponibles() {
        return inventario.getItemsDelInventario();
    }

    public String getTipoComportamientoDefecto() {
        return comportamientoPorDefecto.getTipo();
    }
}
