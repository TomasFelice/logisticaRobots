package com.alphaone.logisticaRobots.domain.entities;

import java.util.Objects;

public class Item {
    private final String nombre;
    private int cantidad;

    public Item(String nombre, int cantidad) {
        this.nombre = nombre;
        this.cantidad = cantidad;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void agregar(int cantidad) throws IllegalArgumentException {
        if( cantidad < 0) {
            throw new IllegalArgumentException("Cantidad debe ser positiva");
        }

        this.cantidad += cantidad;
    }

    public void restar(int cantidad) {
        if ( cantidad < 0) {
            throw new IllegalArgumentException("Cantidad debe ser positiva");
        }

        this.cantidad -= cantidad;
    }

    public boolean esVacio() {
        return cantidad == 0;
    }

    @Override
    public String toString() {
        return nombre + " " + cantidad;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Item other = (Item) obj;
        return Objects.equals(this.nombre, other.nombre);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre);
    }
}
