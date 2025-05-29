package com.alphaone.logisticaRobots.application.dto;

import java.util.List;

public record ConfiguracionSimulacionDTO(
    List<RobotDTO> robots,
    List<CofreDTO> cofres,
    List<RobopuertoDTO> robopuertos,
    DimensionGrillaDTO dimensionGrilla,
    int velocidadSimulacion
) {
}
