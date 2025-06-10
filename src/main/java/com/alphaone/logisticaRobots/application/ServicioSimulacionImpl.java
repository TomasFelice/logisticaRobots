package com.alphaone.logisticaRobots.application;

import com.alphaone.logisticaRobots.application.dto.*;
import com.alphaone.logisticaRobots.domain.Robopuerto;
import com.alphaone.logisticaRobots.domain.RobotLogistico;
import com.alphaone.logisticaRobots.domain.pathfinding.Punto;
import com.alphaone.logisticaRobots.domain.CofreLogistico;
import com.alphaone.logisticaRobots.domain.Item;
import com.alphaone.logisticaRobots.ui.ObservadorEstadoSimulacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Implementación del servicio de simulación que gestiona la lógica y el estado del sistema.
 */
public class ServicioSimulacionImpl implements ServicioSimulacion {
    private static final Logger logger = LoggerFactory.getLogger(ServicioSimulacionImpl.class);
    
    // Sistema del dominio [TODO] -> Hay que pasar com.alphaone.logisticaRobots.domain.RedLogistica a Domain
    private RedLogistica redLogistica;
    
    // Configuración
    private File archivoConfigActual;
    private int velocidadSimulacion = 1000; // milisegundos entre ciclos
    
    // com.alphaone.logisticaRobots.domain.Estado de la simulación
    private boolean enEjecucion = false;
    private int cicloActual = 0;
    private String estadoGeneral = "NO_INICIADO";
    private String mensajeEstado = "Sistema no iniciado";
    
    // Dimensiones de la grilla (configurables desde archivo)
    private int anchoGrilla = 50;
    private int altoGrilla = 50;
    
    // Scheduler para ejecución automática
    private ScheduledExecutorService scheduler;
    
    // Observadores
    private final List<ObservadorEstadoSimulacion> observadores = new CopyOnWriteArrayList<>();
    
    // Constructor
    public ServicioSimulacionImpl() {
        // Inicializar objetos necesarios
        this.redLogistica = new RedLogistica(); // O cómo se inicialice en tu implementación
    }
    
    @Override
    public void iniciarSimulacion() {
        if (redLogistica == null || redLogistica.estaVacia()) {
            mensajeEstado = "No se puede iniciar: no hay configuración cargada";
            notificarObservadores();
            return;
        }
        
        if (!enEjecucion) {
            enEjecucion = true;
            estadoGeneral = "INICIADA";
            mensajeEstado = "Simulación iniciada";
            
            // Iniciar ejecución automática en un hilo separado
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(
                    this::cicloSimulacionSeguro,
                    0,
                    velocidadSimulacion,
                    TimeUnit.MILLISECONDS
            );
            
            notificarObservadores();
        }
    }
    
    @Override
    public void pausarSimulacion() {
        if (enEjecucion && scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            enEjecucion = false;
            estadoGeneral = "PAUSADA";
            mensajeEstado = "Simulación pausada";
            notificarObservadores();
        }
    }
    
    @Override
    public void resetearSimulacion() {
        // Detener la simulación si está en marcha
        if (enEjecucion) {
            pausarSimulacion();
        }
        
        // Resetear variables
        cicloActual = 0;
        estadoGeneral = "NO_INICIADO";
        
        // Si hay un archivo de configuración cargado, lo recargamos
        if (archivoConfigActual != null) {
            try {
                cargarConfiguracion(archivoConfigActual);
                mensajeEstado = "Simulación reseteada y configuración recargada";
            } catch (Exception e) {
                mensajeEstado = "Error al resetear: " + e.getMessage();
                logger.error("Error al resetear la simulación", e);
            }
        } else {
            redLogistica = new RedLogistica(); // O como se reinicie
            mensajeEstado = "Simulación reseteada";
        }
        
        notificarObservadores();
    }
    
    @Override
    public void avanzarCicloSimulacion() {
        if (redLogistica == null || redLogistica.estaVacia()) {
            mensajeEstado = "No se puede avanzar: no hay configuración cargada";
            notificarObservadores();
            return;
        }
        
        if (!enEjecucion) { // Solo avanzamos manualmente si no está en ejecución automática
            cicloSimulacionSeguro();
        }
    }
    
    // Metodo que ejecuta un ciclo de simulación con manejo de excepciones
    private void cicloSimulacionSeguro() {
        try {
            ejecutarCicloSimulacion();
            notificarObservadores();
        } catch (Exception e) {
            pausarSimulacion(); // Detener si hay error
            estadoGeneral = "ERROR";
            mensajeEstado = "Error en ciclo: " + e.getMessage();
            logger.error("Error durante el ciclo de simulación", e);
            notificarObservadores();
        }
    }
    
    // Implementación real del ciclo
    private void ejecutarCicloSimulacion() {
        // Aquí iría la llamada al metodo simularCiclo() de com.alphaone.logisticaRobots.domain.RedLogistica
        redLogistica.simularCiclo();
        
        cicloActual++;
        
        // Verificar si se alcanzó un estado estable
        if (redLogistica.haAlcanzadoEstadoEstable()) {
            pausarSimulacion();
            estadoGeneral = "FINALIZADA_ESTABLE";
            mensajeEstado = "Simulación finalizada: estado estable alcanzado";
        } else {
            estadoGeneral = "CORRIENDO";
            mensajeEstado = "Ciclo " + cicloActual + " completado";
        }
    }
    
    @Override
    public void cargarConfiguracion(File archivoConfig) throws Exception {
        Objects.requireNonNull(archivoConfig, "El archivo no puede ser null");
        
        if (!archivoConfig.exists() || !archivoConfig.canRead()) {
            throw new IllegalArgumentException("Archivo no existe o no se puede leer: " + archivoConfig.getPath());
        }
        
        try {
            ObjectMapper mapper = new ObjectMapper();
            ConfiguracionSimulacionDTO configuracion = mapper.readValue(archivoConfig, ConfiguracionSimulacionDTO.class);
            
            // Crear nueva red y cargar según la configuración
            this.redLogistica = new RedLogistica(); // O como corresponda
            
            // Configurar dimensiones de la grilla
            if (configuracion.dimensionGrilla() != null) {
                this.anchoGrilla = configuracion.dimensionGrilla().ancho();
                this.altoGrilla = configuracion.dimensionGrilla().alto();
            }
            
            // Cargar robopuertos
            if (configuracion.robopuertos() != null) {
                for (RobopuertoDTO rpDTO : configuracion.robopuertos()) {
                    Punto posicion = new Punto(rpDTO.posicion().x(), rpDTO.posicion().y());
                    Robopuerto robopuerto = new Robopuerto(rpDTO.id(), posicion, rpDTO.alcanceCobertura());
                    redLogistica.agregarRobopuerto(robopuerto);
                }
            }
            
            // Cargar cofres
            if (configuracion.cofres() != null) {
                for (CofreDTO cofreDTO : configuracion.cofres()) {
                    Punto posicion = new Punto(cofreDTO.posicion().x(), cofreDTO.posicion().y());
                    CofreLogistico cofre = crearCofreSegunTipo(
                            cofreDTO.id(), posicion, cofreDTO.capacidadMaxima(), 
                            cofreDTO.comportamientoDefecto(), cofreDTO.tiposComportamientoPorItem());
                    
                    // Cargar inventario inicial si existe
                    if (cofreDTO.inventario() != null) {
                        for (Map.Entry<String, Integer> entry : cofreDTO.inventario().entrySet()) {
                            Item item = new Item(entry.getKey());
                            cofre.agregarItem(item, entry.getValue());
                        }
                    }
                    
                    redLogistica.agregarCofre(cofre);
                }
            }
            
            // Cargar robots
            if (configuracion.robots() != null) {
                for (RobotDTO robotDTO : configuracion.robots()) {
                    Punto posicion = new Punto(robotDTO.posicion().x(), robotDTO.posicion().y());
                    // Encontrar el robopuerto base para este robot
                    Robopuerto robopuertoBase = redLogistica.getRobopuertoMasCercano(posicion);
                    if (robopuertoBase == null) {
                        throw new IllegalStateException("No se encontró robopuerto cercano para robot en " + posicion);
                    }
                    
                    RobotLogistico robot = new RobotLogistico(
                            Integer.parseInt(robotDTO.id()), 
                            posicion, 
                            robopuertoBase,
                            (int) robotDTO.bateriaMaxima(),
                            robotDTO.capacidadCarga());
                    
                    redLogistica.agregarRobot(robot);
                }
            }
            
            // Establecer velocidad de simulación si está configurada
            velocidadSimulacion = configuracion.velocidadSimulacion() > 0 ? 
                    configuracion.velocidadSimulacion() : 1000; // valor por defecto
            
            // Guardar referencia al archivo actual
            this.archivoConfigActual = archivoConfig;
            
            // Actualizar estado
            cicloActual = 0;
            estadoGeneral = "NO_INICIADO";
            mensajeEstado = "Configuración cargada exitosamente";
            
            notificarObservadores();
            
            logger.info("Configuración cargada desde {}", archivoConfig.getPath());
            
        } catch (Exception e) {
            logger.error("Error al cargar la configuración desde {}", archivoConfig.getPath(), e);
            throw new Exception("Error al cargar configuración: " + e.getMessage(), e);
        }
    }
    
    private CofreLogistico crearCofreSegunTipo(String id, Punto posicion, int capacidad, 
                                                String comportamientoPorDefecto, 
                                                Map<String, String> comportamientosPorItem) {
        // Aquí iría la lógica para crear el tipo de cofre adecuado
        // Probablemente delegarías esto a una factory o clase especializada
        
        // Código simplificado para el ejemplo:
        CofreLogistico cofre = CofreFactory.crearCofre(id, posicion, capacidad, comportamientoPorDefecto);
        
        if (comportamientosPorItem != null) {
            for (Map.Entry<String, String> entry : comportamientosPorItem.entrySet()) {
                Item item = new Item(entry.getKey());
                ComportamientoCofre comportamiento = ComportamientoFactory.crear(entry.getValue());
                cofre.setComportamiento(item, comportamiento);
            }
        }
        
        return cofre;
    }
    
    @Override
    public EstadoSimulacionDTO getEstadoActualSimulacion() {
        if (redLogistica == null || redLogistica.estaVacia()) {
            return new EstadoSimulacionDTO(
                    new ArrayList<>(), 
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new DimensionGrillaDTO(anchoGrilla, altoGrilla),
                    estadoGeneral,
                    cicloActual,
                    mensajeEstado
            );
        }
        
        // Convertir las entidades del dominio a DTOs
        List<RobotDTO> robotDTOs = convertirRobotsADTOs();
        List<CofreDTO> cofreDTOs = convertirCofresADTOs();
        List<RobopuertoDTO> robopuertoDTOs = convertirRobopuertosADTOs();
        
        return new EstadoSimulacionDTO(
                robotDTOs,
                cofreDTOs,
                robopuertoDTOs,
                new DimensionGrillaDTO(anchoGrilla, altoGrilla),
                estadoGeneral,
                cicloActual,
                mensajeEstado
        );
    }
    
    private List<RobotDTO> convertirRobotsADTOs() {
        List<RobotDTO> resultado = new ArrayList<>();
        
        for (RobotLogistico robot : redLogistica.getRobots()) {
            Punto posicion = robot.getPosicion();
            PuntoDTO posicionDTO = new PuntoDTO(posicion.getX(), posicion.getY());
            
            // Convertir carga a Map<String, Integer>
            Map<String, Integer> itemsEnCarga = new HashMap<>();
            for (Map.Entry<Item, Integer> entry : robot.getCargaActual().entrySet()) {
                itemsEnCarga.put(entry.getKey().getNombre(), entry.getValue());
            }
            
            // Convertir la ruta si existe
            List<PuntoDTO> rutaDTO = null;
            List<Punto> ruta = robot.getRutaActual();
            if (ruta != null && !ruta.isEmpty()) {
                rutaDTO = ruta.stream()
                        .map(p -> new PuntoDTO(p.getX(), p.getY()))
                        .toList();
            }
            
            RobotDTO robotDTO = new RobotDTO(
                    String.valueOf(robot.getId()),
                    posicionDTO,
                    robot.getBateriaActual(),
                    robot.getBateriaMaxima(),
                    robot.getTotalCargaActual(), // Supongo que hay un metodo así
                    robot.getCapacidadCarga(),
                    itemsEnCarga,
                    robot.getEstado().toString(),
                    rutaDTO
            );
            
            resultado.add(robotDTO);
        }
        
        return resultado;
    }
    
    private List<CofreDTO> convertirCofresADTOs() {
        List<CofreDTO> resultado = new ArrayList<>();
        
        for (CofreLogistico cofre : redLogistica.getCofres()) {
            Punto posicion = cofre.getPosicion();
            PuntoDTO posicionDTO = new PuntoDTO(posicion.getX(), posicion.getY());
            
            // Convertir inventario a Map<String, Integer>
            Map<String, Integer> inventarioDTO = new HashMap<>();
            for (Map.Entry<Item, Integer> entry : cofre.getInventario().getTodos().entrySet()) {
                inventarioDTO.put(entry.getKey().getNombre(), entry.getValue());
            }
            
            // Convertir comportamientos a Map<String, String>
            Map<String, String> comportamientosDTO = new HashMap<>();
            for (Item item : cofre.getInventario().getItems()) {
                comportamientosDTO.put(item.getNombre(), cofre.getTipoComportamiento(item));
            }
            
            CofreDTO cofreDTO = new CofreDTO(
                    cofre.getId(),
                    posicionDTO,
                    inventarioDTO,
                    cofre.getInventario().getTotalItems(),
                    cofre.getCapacidadMaxima(),
                    comportamientosDTO,
                    cofre.getTipoComportamientoDefecto(),
                    redLogistica.esCofreAccesible(cofre) // Metodo que indica si está en zona de cobertura
            );
            
            resultado.add(cofreDTO);
        }
        
        return resultado;
    }
    
    private List<RobopuertoDTO> convertirRobopuertosADTOs() {
        List<RobopuertoDTO> resultado = new ArrayList<>();
        
        for (Robopuerto rp : redLogistica.getRobopuertos()) {
            Punto posicion = rp.getPosicion();
            PuntoDTO posicionDTO = new PuntoDTO(posicion.getX(), posicion.getY());
            
            // Obtener los IDs de cofres cubiertos
            List<String> idsCofresCubiertos = redLogistica.getCofresEnCobertura(rp).stream()
                    .map(CofreLogistico::getId)
                    .toList();
            
            RobopuertoDTO rpDTO = new RobopuertoDTO(
                    rp.getId(),
                    posicionDTO,
                    rp.getAlcanceCobertura(),
                    idsCofresCubiertos
            );
            
            resultado.add(rpDTO);
        }
        
        return resultado;
    }
    
    @Override
    public DetallesEntidadDTO getDetallesEntidad(String idEntidad) {
        // Buscar robot
        RobotLogistico robot = redLogistica.buscarRobotPorId(idEntidad);
        if (robot != null) {
            RobotDTO robotDTO = obtenerRobotDTO(robot);
            return new DetallesEntidadDTO(robotDTO);
        }
        
        // Buscar cofre
        CofreLogistico cofre = redLogistica.buscarCofrePorId(idEntidad);
        if (cofre != null) {
            CofreDTO cofreDTO = obtenerCofreDTO(cofre);
            return new DetallesEntidadDTO(cofreDTO);
        }
        
        // Buscar robopuerto
        Robopuerto robopuerto = redLogistica.buscarRobopuertoPorId(idEntidad);
        if (robopuerto != null) {
            RobopuertoDTO rpDTO = obtenerRobopuertoDTO(robopuerto);
            return new DetallesEntidadDTO(rpDTO);
        }
        
        return null; // No se encontró la entidad
    }
    
    // Métodos auxiliares para convertir entidades individuales a DTOs
    
    private RobotDTO obtenerRobotDTO(RobotLogistico robot) {
        // Código similar a convertirRobotsADTOs pero para un solo robot
        // ... (implementación similar)
        return convertirRobotsADTOs().stream()
                .filter(r -> r.id().equals(String.valueOf(robot.getId())))
                .findFirst()
                .orElse(null);
    }
    
    private CofreDTO obtenerCofreDTO(CofreLogistico cofre) {
        // ... (implementación similar)
        return convertirCofresADTOs().stream()
                .filter(c -> c.id().equals(cofre.getId()))
                .findFirst()
                .orElse(null);
    }
    
    private RobopuertoDTO obtenerRobopuertoDTO(Robopuerto robopuerto) {
        // ... (implementación similar)
        return convertirRobopuertosADTOs().stream()
                .filter(rp -> rp.id().equals(robopuerto.getId()))
                .findFirst()
                .orElse(null);
    }
    
    @Override
    public void registrarObservador(ObservadorEstadoSimulacion observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
        }
    }
    
    @Override
    public void removerObservador(ObservadorEstadoSimulacion observador) {
        observadores.remove(observador);
    }
    
    private void notificarObservadores() {
        EstadoSimulacionDTO estadoActual = getEstadoActualSimulacion();
        for (ObservadorEstadoSimulacion observador : observadores) {
            observador.actualizarEstado(estadoActual);
        }
    }
    
    // Para fines de implementación, asumimos estas clases auxiliares
    private static class ConfiguracionSimulacionDTO {
        private List<RobotDTO> robots;
        private List<CofreDTO> cofres;
        private List<RobopuertoDTO> robopuertos;
        private DimensionGrillaDTO dimensionGrilla;
        private int velocidadSimulacion;
        
        // Getters necesarios como métodos
        public List<RobotDTO> robots() { return robots; }
        public List<CofreDTO> cofres() { return cofres; }
        public List<RobopuertoDTO> robopuertos() { return robopuertos; }
        public DimensionGrillaDTO dimensionGrilla() { return dimensionGrilla; }
        public int velocidadSimulacion() { return velocidadSimulacion; }
    }
    
    // Clases auxiliares para crear entidades de dominio según tipo
    private static class CofreFactory {
        public static CofreLogistico crearCofre(String id, Punto posicion, int capacidad, String tipo) {
            // Implementación pendiente [TODO]
            return null; // Placeholder
        }
    }
    
    private static class ComportamientoFactory {
        public static ComportamientoCofre crear(String tipo) {
            // Implementación pendiente [TODO]
            return null; // Placeholder
        }
    }
}