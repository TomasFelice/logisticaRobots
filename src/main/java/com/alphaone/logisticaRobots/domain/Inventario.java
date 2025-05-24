package com.alphaone.logisticaRobots.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Gestiona el inventario de ítems y sus cantidades
 */
public class Inventario {
    private final Map<Item, Integer> items = new HashMap<>();

    public void agregar(Item item, int cantidad) throws IllegalArgumentException {
        if (cantidad <= 0) throw new IllegalArgumentException("Cantidad debe ser mayor a 0");
        items.merge(item, cantidad, Integer::sum);
    }

    public boolean remover(Item item, int cantidad) {
        if (cantidad <= 0) return false;
        Integer actual = items.get(item);
        if (actual == null || actual < cantidad) return false;

        if(actual == cantidad) {
            items.remove(item);
        } else {
            items.put(item, actual - cantidad);
        }

        return true;
    }

    public int getCantidad(Item item) {
        return items.getOrDefault(item, 0);
    }

    public boolean tiene(Item item, int cantidad) {
        return getCantidad(item) >= cantidad;
    }

    public Set<Item> getItems() {
        return items.keySet();
    }

    public Map<Item, Integer> getTodos() {
        return new HashMap<>(items);
    }

    public boolean estaVacio() {
        return items.isEmpty();
    }

    public int getTotalItems() {
        return items.values().stream().mapToInt(Integer::intValue).sum();
    }

    @Override
    public String toString() {
        return "Inventario{items=" + items + "}";
    }
}
