package com.alphaone.logisticaRobots.domain;

import com.alphaone.logisticaRobots.domain.pathfinding.Punto;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Robopuerto implements Ubicable {
    private final String id;
    private Punto posicion;
    private final double distanciaMaxima;
    private final int tasaRecarga;  // Cantidad de células que recarga por ciclo

    // TODO: Nos hace falta tener los asociados? Creo que es mejor manejarlo con alcance
    private final List<RobotLogistico> robotsAsociados;
    private final List<CofreLogistico> cofresConectados;
    private final double alcance;

    public Robopuerto(String id, Punto posicion, double distanciaMaxima, int tasaRecarga, double alcance) {
        this.id = id;
        this.posicion = Objects.requireNonNull(posicion,"Posición no puede ser null");
        this.distanciaMaxima = validarDistancia(distanciaMaxima);
        this.tasaRecarga = validarTasaRecarga(tasaRecarga);
        this.cofresConectados = new ArrayList<>();
        this.robotsAsociados = new ArrayList<>();
        this.alcance = alcance;
    }

    @Override
    public Punto getPosicion() {
        return posicion;
    }

    @Override
    public void setPosicion(Punto posicion) {
        this.posicion = posicion;
    }

    //INICIO - esto tal vez debamos ponerlo en una clase superior porque muchas clases validan distancia

    private double validarDistancia(double distancia) {
        if (distancia <= 0) {
            throw new IllegalArgumentException("La distancia máxima debe ser positiva");
        }
        return distancia;
    }

    public boolean estaEnCobertura(Punto otraUbicacion) {
        return posicion.distanciaHacia(otraUbicacion) <= distanciaMaxima;
    }

    //FIN - esto tal vez debamos ponerlo en una clase superior porque muchas clases validan distancia

    // Manejo de Cofres:

    public void conectarCofre(CofreLogistico cofre) {
        if (estaEnCobertura(cofre.getPosicion())) {
            cofresConectados.add(cofre);
        } else {
            throw new IllegalArgumentException("El cofre está fuera del área de cobertura");
        }
    }

    public void desconectarCofre(CofreLogistico cofre) {
        if (!cofresConectados.remove(cofre)) {
            throw new IllegalArgumentException("El cofre no estaba conectado a este robopuerto");
        }
    }

    //Manejo de robots:

    public void conectarRobot(RobotLogistico robot) {
        if(!robotsAsociados.contains(robot)) {
            robotsAsociados.add(robot);
        }
    }

    public void desconectarRobot(RobotLogistico robot) {
        if (robotsAsociados.contains(robot)) {
            robotsAsociados.remove(robot);
        }
    }

    private int validarTasaRecarga(int tasaRecarga) {
        if (tasaRecarga <= 0) {
            throw new IllegalArgumentException("La distancia debe ser positiva");
        }
        return tasaRecarga;
    }

    public void recargarRobot(RobotLogistico robot) { // Podríamos hacer un exception acá como: RecargaNoPermitidaException?
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