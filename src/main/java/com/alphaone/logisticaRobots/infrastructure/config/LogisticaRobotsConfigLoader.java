package com.alphaone.logisticaRobots.infrastructure.config;

import com.alphaone.logisticaRobots.application.dto.ConfiguracionSimulacionDTO;
import com.alphaone.logisticaRobots.application.dto.CofreDTO;
import com.alphaone.logisticaRobots.application.dto.DimensionGrillaDTO;
import com.alphaone.logisticaRobots.application.dto.ItemCantidadDTO;
import com.alphaone.logisticaRobots.application.dto.PedidoDTO;
import com.alphaone.logisticaRobots.application.dto.PuntoDTO;
import com.alphaone.logisticaRobots.application.dto.RobopuertoDTO;
import com.alphaone.logisticaRobots.application.dto.RobotDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Clase encargada de cargar el archivo de configuración de logística de robots en memoria.
 * Esta clase proporciona métodos para acceder a la configuración cargada desde el archivo JSON.
 */
public class LogisticaRobotsConfigLoader {

    private static final Logger logger = LoggerFactory.getLogger(LogisticaRobotsConfigLoader.class);
    private static final String CONFIG_FILE_PATH = "/config/redConectadaCompletaCumplible.json";

    private ConfiguracionSimulacionDTO configuracion;
    private final ObjectMapper objectMapper;

    /**
     * Constructor por defecto que inicializa el ObjectMapper.
     */
    public LogisticaRobotsConfigLoader() {
        this.objectMapper = new ObjectMapper();
        // Configurar ObjectMapper para ignorar propiedades desconocidas
        objectMapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Carga la configuración desde el archivo JSON predeterminado.
     * 
     * @return La configuración cargada
     * @throws IOException Si ocurre un error al leer o parsear el archivo
     */
    public ConfiguracionSimulacionDTO cargarConfiguracion() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream(CONFIG_FILE_PATH)) {
            if (inputStream == null) {
                throw new IOException("No se pudo encontrar el archivo de configuración: " + CONFIG_FILE_PATH);
            }

            JsonNode rootNode = objectMapper.readTree(inputStream);
            configuracion = mapearJsonAConfiguracion(rootNode);
            logger.info("Configuración cargada exitosamente desde {}", CONFIG_FILE_PATH);
            return configuracion;
        } catch (IOException e) {
            logger.error("Error al cargar la configuración desde {}", CONFIG_FILE_PATH, e);
            throw e;
        }
    }

    /**
     * Carga la configuración desde un archivo específico.
     * 
     * @param archivoConfig El archivo de configuración a cargar
     * @return La configuración cargada
     * @throws IOException Si ocurre un error al leer o parsear el archivo
     */
    public ConfiguracionSimulacionDTO cargarConfiguracion(File archivoConfig) throws IOException {
        Objects.requireNonNull(archivoConfig, "El archivo no puede ser null");

        if (!archivoConfig.exists() || !archivoConfig.canRead()) {
            throw new IllegalArgumentException("Archivo no existe o no se puede leer: " + archivoConfig.getPath());
        }

        try {
            JsonNode rootNode = objectMapper.readTree(archivoConfig);
            configuracion = mapearJsonAConfiguracion(rootNode);
            logger.info("Configuración cargada exitosamente desde {}", archivoConfig.getPath());
            return configuracion;
        } catch (IOException e) {
            logger.error("Error al cargar la configuración desde {}", archivoConfig.getPath(), e);
            throw e;
        }
    }

    /**
     * Mapea el JSON a un objeto ConfiguracionSimulacionDTO.
     * 
     * @param rootNode El nodo raíz del JSON
     * @return El objeto ConfiguracionSimulacionDTO mapeado
     */
    private ConfiguracionSimulacionDTO mapearJsonAConfiguracion(JsonNode rootNode) {
        // Mapear dimensiones de la grilla
        DimensionGrillaDTO dimensionGrilla = null;
        if (rootNode.has("grilla") && rootNode.get("grilla").has("dimensiones")) {
            JsonNode dimensionesNode = rootNode.get("grilla").get("dimensiones");
            int ancho = dimensionesNode.has("ancho") ? dimensionesNode.get("ancho").asInt() : 10;
            int alto = dimensionesNode.has("alto") ? dimensionesNode.get("alto").asInt() : 10;
            dimensionGrilla = new DimensionGrillaDTO(ancho, alto);
        } else {
            // Valores por defecto si no se encuentra la información
            dimensionGrilla = new DimensionGrillaDTO(10, 10);
        }

        // Mapear robopuertos
        List<RobopuertoDTO> robopuertos = new ArrayList<>();
        if (rootNode.has("robopuertos") && rootNode.get("robopuertos").isArray()) {
            ArrayNode robopuertosArray = (ArrayNode) rootNode.get("robopuertos");
            for (JsonNode rpNode : robopuertosArray) {
                String id = rpNode.has("id") ? rpNode.get("id").asText() : "";
                double alcance = rpNode.has("alcance") ? rpNode.get("alcance").asDouble() : 0;
                int tasaRecarga = 1; // Valor por defecto

                // Mapear posición
                PuntoDTO posicion = null;
                if (rpNode.has("posicion")) {
                    JsonNode posicionNode = rpNode.get("posicion");
                    int x = posicionNode.has("x") ? posicionNode.get("x").asInt() : 0;
                    int y = posicionNode.has("y") ? posicionNode.get("y").asInt() : 0;
                    posicion = new PuntoDTO(x, y);
                } else {
                    posicion = new PuntoDTO(0, 0);
                }

                // Crear el DTO de robopuerto
                RobopuertoDTO robopuertoDTO = new RobopuertoDTO(id, posicion, alcance, tasaRecarga, new ArrayList<>());
                robopuertos.add(robopuertoDTO);
            }
        }

        // Mapear robots
        List<RobotDTO> robots = new ArrayList<>();
        if (rootNode.has("robots") && rootNode.get("robots").isArray()) {
            ArrayNode robotsArray = (ArrayNode) rootNode.get("robots");
            int idRobot = 1; // Contador para asignar IDs
            for (JsonNode robotNode : robotsArray) {
                String id = String.valueOf(idRobot++);
                double bateriaMaxima = robotNode.has("capacidadMaximaBateria") ? 
                        robotNode.get("capacidadMaximaBateria").asDouble() : 100;
                double bateriaActual = robotNode.has("cargaBateriaActual") ? 
                        robotNode.get("cargaBateriaActual").asDouble() : bateriaMaxima;
                int capacidadCarga = robotNode.has("capacidadTraslado") ? 
                        robotNode.get("capacidadTraslado").asInt() : 5;
                String estado = robotNode.has("estado") ? 
                        robotNode.get("estado").asText() : "disponible";

                // Posición inicial (se asume en el robopuerto inicial)
                PuntoDTO posicion = new PuntoDTO(0, 0);
                String robopuertoBaseId = null;
                if (robotNode.has("robopuertoInicialId") && !robopuertos.isEmpty()) {
                    robopuertoBaseId = robotNode.get("robopuertoInicialId").asText();
                    // Buscar el robopuerto por ID
                    for (RobopuertoDTO rp : robopuertos) {
                        if (rp.id().equals(robopuertoBaseId)) {
                            posicion = rp.posicion();
                            break;
                        }
                    }
                }

                // Crear el DTO de robot
                RobotDTO robotDTO = new RobotDTO(
                        id, 
                        posicion, 
                        bateriaActual, 
                        bateriaMaxima, 
                        (int)bateriaActual, 
                        capacidadCarga, 
                        new HashMap<>(), 
                        estado, 
                        null,
                        robopuertoBaseId
                );
                robots.add(robotDTO);
            }
        }

        // Mapear cofres
        List<CofreDTO> cofres = new ArrayList<>();
        if (rootNode.has("cofres") && rootNode.get("cofres").isArray()) {
            ArrayNode cofresArray = (ArrayNode) rootNode.get("cofres");
            for (JsonNode cofreNode : cofresArray) {
                String id = cofreNode.has("id") ?
                        cofreNode.get("id").asText() : "";
                int capacidadMaxima = cofreNode.has("capacidadMaxima") ? 
                        cofreNode.get("capacidadMaxima").asInt() : 20;

                // Mapear posición
                PuntoDTO posicion = null;
                if (cofreNode.has("posicion")) {
                    JsonNode posicionNode = cofreNode.get("posicion");
                    int x = posicionNode.has("x") ? posicionNode.get("x").asInt() : 0;
                    int y = posicionNode.has("y") ? posicionNode.get("y").asInt() : 0;
                    posicion = new PuntoDTO(x, y);
                } else {
                    posicion = new PuntoDTO(0, 0);
                }

                // Mapear inventario
                Map<String, Integer> inventario = new HashMap<>();
                if (cofreNode.has("itemsAlmacenados") && cofreNode.get("itemsAlmacenados").isArray()) {
                    ArrayNode itemsArray = (ArrayNode) cofreNode.get("itemsAlmacenados");
                    for (JsonNode itemNode : itemsArray) {
                        String item = itemNode.has("item") ? itemNode.get("item").asText() : "";
                        int cantidad = itemNode.has("cantidad") ? itemNode.get("cantidad").asInt() : 0;
                        if (!item.isEmpty()) {
                            inventario.put(item, cantidad);
                        }
                    }
                }

                // Mapear comportamientos por item
                Map<String, String> comportamientosPorItem = new HashMap<>();
                if (cofreNode.has("comportamientosPorItem") && cofreNode.get("comportamientosPorItem").isObject()) {
                    JsonNode comportamientosNode = cofreNode.get("comportamientosPorItem");
                    comportamientosNode.fields().forEachRemaining(entry -> {
                        comportamientosPorItem.put(entry.getKey(), entry.getValue().asText());
                    });
                }

                // TODO: Probablemente haya que sacarlo, ya no tiene sentido.
                // Obtener comportamiento por defecto
                String comportamientoDefecto = cofreNode.has("comportamientoDefecto") ? 
                        cofreNode.get("comportamientoDefecto").asText() : "almacenamiento";

                // Crear el DTO de cofre
                CofreDTO cofreDTO = new CofreDTO(
                        id, 
                        posicion, 
                        inventario, 
                        inventario.values().stream().mapToInt(Integer::intValue).sum(), 
                        capacidadMaxima, 
                        comportamientosPorItem, 
                        comportamientoDefecto, 
                        true
                );
                cofres.add(cofreDTO);
            }
        }

        // Mapear pedidos
        List<PedidoDTO> pedidos = new ArrayList<>();
        if (rootNode.has("pedidos") && rootNode.get("pedidos").isArray()) {
            ArrayNode pedidosArray = (ArrayNode) rootNode.get("pedidos");
            for (JsonNode pedidoNode : pedidosArray) {
                String id = pedidoNode.has("id") ? pedidoNode.get("id").asText() : "";
                String itemNombre = pedidoNode.has("itemNombre") ? pedidoNode.get("itemNombre").asText() : "";
                int cantidad = pedidoNode.has("cantidad") ? pedidoNode.get("cantidad").asInt() : 1;
                String cofreDestinoId = pedidoNode.has("cofreDestinoId") ? pedidoNode.get("cofreDestinoId").asText() : "";
                String prioridad = pedidoNode.has("prioridad") ? pedidoNode.get("prioridad").asText() : "MEDIA";

                // Crear el DTO de pedido
                PedidoDTO pedidoDTO = new PedidoDTO(id, itemNombre, cantidad, cofreDestinoId, prioridad);
                pedidos.add(pedidoDTO);
            }
        }

        // Crear y retornar la configuración
        return new ConfiguracionSimulacionDTO(
                robots, 
                cofres, 
                robopuertos, 
                pedidos,
                dimensionGrilla, 
                1000 // Velocidad de simulación por defecto
        );
    }

    /**
     * Obtiene la configuración cargada.
     * 
     * @return La configuración cargada o null si no se ha cargado ninguna configuración
     */
    public ConfiguracionSimulacionDTO getConfiguracion() {
        return configuracion;
    }

    /**
     * Obtiene la ruta del archivo de configuración como URL.
     * 
     * @return La URL del archivo de configuración o null si no se encuentra
     */
    public URL getConfigFileUrl() {
        return getClass().getResource(CONFIG_FILE_PATH);
    }

    /**
     * Obtiene la ruta del archivo de configuración como File.
     * 
     * @return El archivo de configuración o null si no se encuentra
     */
    public File getConfigFile() {
        URL url = getConfigFileUrl();
        if (url != null) {
            try {
                return new File(url.toURI());
            } catch (Exception e) {
                logger.error("Error al convertir URL a File", e);
            }
        }
        return null;
    }
}
