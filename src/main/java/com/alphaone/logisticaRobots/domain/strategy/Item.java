package com.alphaone.logisticaRobots.domain.strategy;

import java.util.Objects;

/**
 * Representa un tipo de recurso en el sistema logístico
 */
public class Item {
    private final String id;
    private final String nombre;

    public Item(String id, String nombre) {
        this.id = Objects.requireNonNull(id, "ID no puede ser null");
        this.nombre = Objects.requireNonNull(nombre, "Nombre no puede ser null");
    }

    public String getId() { return id; }
    public String getNombre() { return nombre; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("Item{id='%s', nombre='%s'}", id, nombre);
    }
}
