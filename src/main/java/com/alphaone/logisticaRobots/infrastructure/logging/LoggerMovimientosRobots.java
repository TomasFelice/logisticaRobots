package com.alphaone.logisticaRobots.infrastructure.logging;

import com.alphaone.logisticaRobots.domain.RobotLogistico;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LoggerMovimientosRobots {
    private static LoggerMovimientosRobots instancia;
    private final Map<Integer, List<String>> logsPorRobot = new HashMap<>();
    private final String nombreArchivo;
    private final File archivo;
    private final Set<RobotLogistico> robotsRegistrados = new HashSet<>();
    private String informacionCofresInaccesibles = "";
    
    private LoggerMovimientosRobots(String nombreArchivo) {
        // Obtener fecha y hora actual
        LocalDateTime ahora = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_yyyy_HH_mm_ss");
        String fechaHora = ahora.format(formatter);
        
        // Crear nombre único con fecha y hora
        String nombreBase = nombreArchivo.replace(".json", "");
        this.nombreArchivo = nombreBase + "_movimientos_" + fechaHora + ".txt";
        
        // Crear el archivo en la carpeta logging
        String loggingPath = "src/main/java/com/alphaone/logisticaRobots/infrastructure/logging/";
        this.archivo = new File(loggingPath + this.nombreArchivo);
        // Crear el directorio si no existe
        archivo.getParentFile().mkdirs();
        // No eliminar archivo existente, permitir múltiples ejecuciones
    }
    
    public static LoggerMovimientosRobots getInstancia(String nombreArchivo) {
        if (instancia == null) instancia = new LoggerMovimientosRobots(nombreArchivo);
        return instancia;
    }
    
    public static LoggerMovimientosRobots getInstancia() {
        if (instancia == null) throw new IllegalStateException("Logger no inicializado");
        return instancia;
    }
    
    public synchronized void logMovimiento(int idRobot, int ciclo, String pedido, String accion, String posAnterior, String posNueva, double distancia, String bateria) {
        String linea = String.format("  Ciclo %d: %s, Pedido: %s, De: %s A: %s, Distancia: %.2f, Batería: %s", ciclo, accion, pedido, posAnterior, posNueva, distancia, bateria);
        logsPorRobot.computeIfAbsent(idRobot, k -> new ArrayList<>()).add(linea);
    }
    
    public synchronized void registrarRobot(RobotLogistico robot) {
        robotsRegistrados.add(robot);
    }
    
    public synchronized void agregarInformacionCofresInaccesibles(String informacion) {
        this.informacionCofresInaccesibles = informacion;
    }
    
    public synchronized void guardar() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(archivo))) {
            // Obtener fecha y hora para el encabezado
            LocalDateTime ahora = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            String fechaHora = ahora.format(formatter);
            
            // Encabezado del archivo
            pw.println("=".repeat(80));
            pw.println("                    REGISTRO DE MOVIMIENTOS DE ROBOTS");
            pw.println("=".repeat(80));
            pw.println("Fecha y hora de simulación: " + fechaHora);
            pw.println();
            
            // Logs por robot
            for (Map.Entry<Integer, List<String>> entry : logsPorRobot.entrySet()) {
                pw.println("Robot " + entry.getKey() + ":");
                pw.println("-".repeat(40));
                for (String linea : entry.getValue()) {
                    pw.println(linea);
                }
                pw.println();
            }
            
            // Sección de fin de pedidos
            pw.println("=".repeat(80));
            pw.println("                           FIN DE PEDIDOS");
            pw.println("=".repeat(80));
            pw.println();
            
            // Información de cofres inaccesibles si existe
            if (!informacionCofresInaccesibles.isEmpty()) {
                pw.println("=".repeat(80));
                pw.println("                    COFRES INACCESIBLES");
                pw.println("=".repeat(80));
                pw.println();
                pw.println(informacionCofresInaccesibles);
                pw.println();
            }
            
            // Resumen de estados de robots
            pw.println("RESUMEN FINAL DE ESTADOS DE ROBOTS:");
            pw.println("-".repeat(50));
            pw.println();
            
            for (RobotLogistico robot : robotsRegistrados) {
                pw.println("Robot " + robot.getId() + ":");
                pw.println("   Posición: " + robot.getPosicion());
                pw.println("   Batería: " + robot.getBateriaActual() + "/" + robot.getBateriaMaxima());
                pw.println("   Estado: " + robot.getEstado());
                pw.println("   Pedidos completados: " + robot.getHistorialPedidos().size());
                pw.println("   Pedidos pendientes: " + robot.getPedidosPendientes().size());
                pw.println("   Pedidos en proceso: " + robot.getCantidadPedidosEnProceso());
                pw.println();
            }
            
            // Estadísticas generales
            pw.println("ESTADÍSTICAS GENERALES:");
            pw.println("-".repeat(30));
            int totalPedidosCompletados = robotsRegistrados.stream()
                .mapToInt(r -> r.getHistorialPedidos().size())
                .sum();
            int totalPedidosPendientes = robotsRegistrados.stream()
                .mapToInt(r -> r.getPedidosPendientes().size())
                .sum();
            int totalMovimientos = logsPorRobot.values().stream()
                .mapToInt(List::size)
                .sum();
            
            pw.println("   Total pedidos completados: " + totalPedidosCompletados);
            pw.println("   Total pedidos pendientes: " + totalPedidosPendientes);
            pw.println("   Total movimientos registrados: " + totalMovimientos);
            pw.println("   Total robots activos: " + robotsRegistrados.size());
            pw.println();
            
            pw.println("=".repeat(80));
            pw.println("                    SIMULACIÓN FINALIZADA");
            pw.println("=".repeat(80));
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
} 