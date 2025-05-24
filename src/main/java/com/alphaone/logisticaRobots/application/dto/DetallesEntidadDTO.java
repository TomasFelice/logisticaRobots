package com.alphaone.logisticaRobots.application.dto;

/**
 * DTO para transportar detalles de una entidad seleccionada.
 * Puede ser un robot, cofre o robopuerto. Se usa un campo para cada tipo posible,
 * donde solo uno de ellos estará poblado.
 */
public record DetallesEntidadDTO(
        RobotDTO robot,
        CofreDTO cofre,
        RobopuertoDTO robopuerto,
        String tipoEntidad // "ROBOT", "COFRE", "ROBOPUERTO"
) {
    // Constructores convenientes para cada tipo de entidad
    public DetallesEntidadDTO(RobotDTO robot) {
        this(robot, null, null, "ROBOT");
    }

    public DetallesEntidadDTO(CofreDTO cofre) {
        this(null, cofre, null, "COFRE");
    }

    public DetallesEntidadDTO(RobopuertoDTO robopuerto) {
        this(null, null, robopuerto, "ROBOPUERTO");
    }
}

