package com.alphaone.logisticaRobots.domain;

/**
 * TODO: Revisar estados ACTIVO y PASIVO. Tentativa -> Sacar PASIVO
 */
public enum EstadoRobot {
    ACTIVO,      // Robot operativo y listo para recibir órdenes
    PASIVO,      // Robot en espera pero operativo -
    INACTIVO,    // Robot fuera de servicio
    EN_MISION,   // Robot ejecutando una tarea
    CARGANDO    // Robot recargando batería
}