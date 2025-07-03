package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.Punto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Robopuerto implements Ubicable {
    private final String id;
    private Punto posicion;
    private final double alcance;
    private final int tasaRecarga;  // Cantidad de células que recarga por ciclo

    private final List<CofreLogistico> cofresConectados;

    public Robopuerto(String id, Punto posicion, double alcance, int tasaRecarga) {
        this.id = id;
        this.posicion = Objects.requireNonNull(posicion,"Posición no puede ser null");
        this.alcance = validarDistancia(alcance);
        this.tasaRecarga = validarTasaRecarga(tasaRecarga);
        this.cofresConectados = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public double getAlcance() {
        return alcance;
    }

    public int getTasaRecarga() {
        return tasaRecarga;
    }

    public List<CofreLogistico> getCofresConectados() {
        return cofresConectados;
    }

    @Override
    public Punto getPosicion() {
        return posicion;
    }

    @Override
    public void setPosicion(Punto posicion) {
        this.posicion = posicion;
    }

    private double validarDistancia(double distancia) {
        if (distancia <= 0) {
            throw new IllegalArgumentException("La distancia máxima debe ser positiva");
        }
        return distancia;
    }

    public boolean estaEnCobertura(Punto otraUbicacion) {
        return posicion.distanciaHacia(otraUbicacion) <= alcance;
    }

    // Manejo de Cofres:

    public void conectarCofre(CofreLogistico cofre) {
        if (estaEnCobertura(cofre.getPosicion())) {
            cofresConectados.add(cofre);
        } else {
            throw new IllegalArgumentException("El cofre está fuera del área de cobertura");
        }
    }

    //Manejo de robots:

    private int validarTasaRecarga(int tasaRecarga) {
        if (tasaRecarga <= 0) {
            throw new IllegalArgumentException("La distancia debe ser positiva");
        }
        return tasaRecarga;
    }

    public void recargarRobot(RobotLogistico robot) { // ¿Podríamos hacer un exception acá como: RecargaNoPermitidaException?
        if (!robot.getPosicion().equals(this.posicion)) {
            throw new IllegalStateException("El robot no está en la posición del robopuerto para ser cargado");
        }

        if (robot.getBateriaActual() >= robot.getBateriaMaxima()) {
            throw new IllegalStateException("El robot ya tiene la batería completa");
        }

        if (!robot.estadoValidoDeRecarga(robot.getEstado())) {
            throw new IllegalStateException("El robot no está en un estado válido para recargar. com.alphaone.logisticaRobots.domain.Estado actual: "
                    + robot.getEstado());
        }

        robot.recargarBateria(this.tasaRecarga);
    }
}