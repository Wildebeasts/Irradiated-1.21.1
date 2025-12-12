package com.momosoftworks.irradiated.common.debug;

import com.momosoftworks.irradiated.api.radiation.RadiationAPI;
import com.momosoftworks.irradiated.common.radiation.DynamicRadiationHandler;
import com.momosoftworks.irradiated.common.radiation.RadiationConfig;
import com.momosoftworks.irradiated.core.init.ModEffects;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

public class RadiationDebugServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RadiationDebugServer.class);

    private static RadiationDebugServer instance;
    private HttpServer httpServer;
    private ServerSocket webSocketServerSocket;
    private final List<WebSocketClient> webSocketClients = new CopyOnWriteArrayList<>();
    private ScheduledExecutorService scheduler;
    private ExecutorService webSocketExecutor;
    private MinecraftServer minecraftServer;
    private volatile boolean running = false;

    private RadiationDebugServer() {
    }

    public static RadiationDebugServer getInstance() {
        if (instance == null) {
            instance = new RadiationDebugServer();
        }
        return instance;
    }

    public void start(MinecraftServer server) {
        if (running) {
            LOGGER.warn("Debug server is already running!");
            return;
        }

        if (!RadiationConfig.ENABLE_DEBUG_SERVER.get()) {
            LOGGER.info("Debug server is disabled in config");
            return;
        }

        this.minecraftServer = server;
        int port = RadiationConfig.DEBUG_SERVER_PORT.get();
        boolean allowRemote = RadiationConfig.DEBUG_SERVER_ALLOW_REMOTE.get();
        String bindAddress = allowRemote ? "0.0.0.0" : "127.0.0.1";

        try {
            // Set running to true BEFORE starting threads
            running = true;

            // Start HTTP server for dashboard
            httpServer = HttpServer.create(new InetSocketAddress(bindAddress, port), 0);
            httpServer.createContext("/", new DashboardHandler());
            httpServer.createContext("/api/config", new ConfigApiHandler());
            httpServer.setExecutor(Executors.newFixedThreadPool(2));
            httpServer.start();

            // Start WebSocket server
            int wsPort = RadiationConfig.DEBUG_WEBSOCKET_PORT.get();
            if (wsPort == 0) {
                // Auto-assign: HTTP port + 1
                wsPort = port + 1;
            }
            webSocketExecutor = Executors.newCachedThreadPool();
            webSocketServerSocket = new ServerSocket(wsPort, 50, java.net.InetAddress.getByName(bindAddress));
            LOGGER.info("WebSocket ServerSocket created and bound to {}:{}", bindAddress, wsPort);
            webSocketExecutor.submit(this::acceptWebSocketConnections);
            LOGGER.info("WebSocket acceptor thread submitted");

            // Start broadcast scheduler
            scheduler = Executors.newSingleThreadScheduledExecutor();
            int intervalMs = RadiationConfig.DEBUG_UPDATE_INTERVAL_MS.get();
            scheduler.scheduleAtFixedRate(this::broadcastRadiationData, 0, intervalMs, TimeUnit.MILLISECONDS);
            LOGGER.warn("===========================================");
            LOGGER.warn("RADIATION DEBUG SERVER STARTED");
            if (allowRemote) {
                LOGGER.warn("Dashboard: http://<server-ip>:{}/", port);
                LOGGER.warn("WebSocket: ws://<server-ip>:{}/", wsPort);
                LOGGER.warn("⚠️  REMOTE ACCESS ENABLED - Use with caution!");
            } else {
                LOGGER.warn("Dashboard: http://localhost:{}/", port);
                LOGGER.warn("WebSocket: ws://localhost:{}/", wsPort);
                LOGGER.warn("Remote access disabled (localhost only)");
            }
            LOGGER.warn("WARNING: Only use for debugging!");
            LOGGER.warn("===========================================");

        } catch (Exception e) {
            LOGGER.error("Failed to start debug server", e);
            stop();
        }
    }

    public void stop() {
        running = false;

        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        if (httpServer != null) {
            httpServer.stop(0);
            httpServer = null;
        }

        // Close all WebSocket clients
        for (WebSocketClient client : webSocketClients) {
            client.close();
        }
        webSocketClients.clear();

        if (webSocketServerSocket != null) {
            try {
                webSocketServerSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            webSocketServerSocket = null;
        }

        if (webSocketExecutor != null) {
            webSocketExecutor.shutdownNow();
            webSocketExecutor = null;
        }

        LOGGER.info("Debug server stopped");
    }

    private void acceptWebSocketConnections() {
        LOGGER.info("WebSocket acceptor thread started, waiting for connections...");
        while (running && webSocketServerSocket != null && !webSocketServerSocket.isClosed()) {
            try {
                LOGGER.debug("Waiting for WebSocket connection on accept()...");
                Socket clientSocket = webSocketServerSocket.accept();
                LOGGER.info("WebSocket connection accepted from: {}", clientSocket.getRemoteSocketAddress());
                WebSocketClient client = new WebSocketClient(clientSocket);
                if (client.performHandshake()) {
                    webSocketClients.add(client);
                    webSocketExecutor.submit(() -> handleWebSocketClient(client));
                    LOGGER.info("WebSocket client connected successfully: {}", clientSocket.getRemoteSocketAddress());
                } else {
                    LOGGER.warn("WebSocket handshake failed for: {}", clientSocket.getRemoteSocketAddress());
                }
            } catch (IOException e) {
                if (running) {
                    LOGGER.error("Error accepting WebSocket connection", e);
                }
            }
        }
        LOGGER.info("WebSocket acceptor thread exiting - running:{} socket:{}",
                running, webSocketServerSocket != null ? (!webSocketServerSocket.isClosed()) : "null");
    }

    private void handleWebSocketClient(WebSocketClient client) {
        try {
            LOGGER.info("Starting WebSocket message loop for client");
            while (running && client.isConnected()) {
                String message = client.readMessage();
                if (message != null) {
                    LOGGER.debug("Received WebSocket message: {}", message);
                    handleWebSocketCommand(client, message);
                }
            }
            LOGGER.info("WebSocket message loop ended - running:{} connected:{}", running, client.isConnected());
        } catch (Exception e) {
            LOGGER.warn("WebSocket client exception: {}", e.getMessage());
        } finally {
            webSocketClients.remove(client);
            client.close();
            LOGGER.info("WebSocket client disconnected");
        }
    }

    private void handleWebSocketCommand(WebSocketClient client, String message) {
        try {
            // Ignore empty keepalive pings
            if (message == null || message.trim().isEmpty()) {
                return;
            }

            if (message.startsWith("{")) {
                // JSON command
                if (message.contains("\"action\":\"set\"")) {
                    String playerName = extractJsonString(message, "player");
                    int level = extractJsonInt(message, "level");
                    executeOnServer(() -> {
                        ServerPlayer player = findPlayer(playerName);
                        if (player != null) {
                            RadiationAPI.setRadiationLevel(player, level, 20 * 60 * 60);
                        }
                    });
                } else if (message.contains("\"action\":\"clear\"")) {
                    String playerName = extractJsonString(message, "player");
                    executeOnServer(() -> {
                        ServerPlayer player = findPlayer(playerName);
                        if (player != null) {
                            RadiationAPI.clearRadiation(player);
                        }
                    });
                } else if (message.contains("\"action\":\"add\"")) {
                    String playerName = extractJsonString(message, "player");
                    int amount = extractJsonInt(message, "amount");
                    executeOnServer(() -> {
                        ServerPlayer player = findPlayer(playerName);
                        if (player != null) {
                            RadiationAPI.addRadiation(player, amount, 20 * 60 * 60);
                        }
                    });
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling WebSocket command: {}", message, e);
        }
    }

    private void executeOnServer(Runnable task) {
        if (minecraftServer != null) {
            minecraftServer.execute(task);
        }
    }

    private ServerPlayer findPlayer(String name) {
        if (minecraftServer == null)
            return null;
        for (ServerPlayer player : minecraftServer.getPlayerList().getPlayers()) {
            if (player.getName().getString().equalsIgnoreCase(name)) {
                return player;
            }
        }
        return null;
    }

    private String extractJsonString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1)
            return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1)
            return "";
        return json.substring(start, end);
    }

    private int extractJsonInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1)
            return 0;
        start += search.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (Character.isDigit(c) || c == '-') {
                sb.append(c);
            } else if (sb.length() > 0) {
                break;
            }
        }
        try {
            return Integer.parseInt(sb.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void broadcastRadiationData() {
        if (!running || webSocketClients.isEmpty() || minecraftServer == null) {
            return;
        }

        try {
            String json = buildRadiationDataJson();
            int successCount = 0;
            java.util.List<WebSocketClient> disconnected = new java.util.ArrayList<>();

            for (WebSocketClient client : webSocketClients) {
                try {
                    if (!client.isConnected()) {
                        disconnected.add(client);
                        continue;
                    }
                    client.sendMessage(json);
                    successCount++;
                } catch (Exception e) {
                    LOGGER.warn("Failed to send to WebSocket client: {}", e.getMessage());
                    disconnected.add(client);
                }
            }

            // Remove disconnected clients
            for (WebSocketClient client : disconnected) {
                webSocketClients.remove(client);
                client.close();
            }

            if (successCount > 0) {
                LOGGER.debug("Broadcast radiation data to {} clients", successCount);
            }
        } catch (Exception e) {
            LOGGER.error("Error broadcasting radiation data", e);
        }
    }

    private String buildRadiationDataJson() {
        StringBuilder json = new StringBuilder();
        json.append("{\"timestamp\":").append(System.currentTimeMillis());
        json.append(",\"players\":[");

        List<ServerPlayer> players = minecraftServer.getPlayerList().getPlayers();
        boolean first = true;

        for (ServerPlayer player : players) {
            if (!first)
                json.append(",");
            first = false;

            json.append("{");
            json.append("\"name\":\"").append(escapeJson(player.getName().getString())).append("\"");
            json.append(",\"uuid\":\"").append(player.getUUID()).append("\"");

            // Position
            BlockPos pos = player.blockPosition();
            json.append(",\"position\":{\"x\":").append(pos.getX());
            json.append(",\"y\":").append(pos.getY());
            json.append(",\"z\":").append(pos.getZ()).append("}");

            // Dimension
            json.append(",\"dimension\":\"").append(player.level().dimension().location()).append("\"");

            // Biome
            String biomeName = getBiomeName(player);
            json.append(",\"biome\":\"").append(escapeJson(biomeName)).append("\"");

            // Radiation data
            int radiationLevel = RadiationAPI.getRadiationLevel(player);
            float dynamicExposure = DynamicRadiationHandler.getCurrentRadiationExposure(player);

            json.append(",\"radiationLevel\":").append(radiationLevel);
            json.append(",\"dynamicExposure\":").append(String.format(java.util.Locale.US, "%.2f", dynamicExposure));

            // Effects and Protection
            boolean hasRadResistance = player.hasEffect(ModEffects.radResistanceHolder());
            int radResistanceAmplifier = 0;
            int radResistanceDuration = 0;
            if (hasRadResistance) {
                var effect = player.getEffect(ModEffects.radResistanceHolder());
                if (effect != null) {
                    radResistanceAmplifier = effect.getAmplifier();
                    radResistanceDuration = effect.getDuration();
                }
            }
            json.append(",\"hasRadResistance\":").append(hasRadResistance);
            json.append(",\"radResistanceLevel\":").append(radResistanceAmplifier + 1);
            json.append(",\"radResistanceDuration\":").append(radResistanceDuration);
            json.append(",\"isInWater\":").append(player.isInWater());

            // Armor protection percentage
            float armorProtection = calculateArmorProtection(player);
            json.append(",\"armorProtection\":").append(String.format(java.util.Locale.US, "%.1f", armorProtection));

            // Nearby radioactive blocks with shielding info
            json.append(",\"nearbyRadioactiveBlocks\":").append(getNearbyRadioactiveBlocksWithShielding(player));

            json.append("}");
        }

        json.append("],\"config\":{");
        json.append("\"blockRadiationRange\":").append(RadiationConfig.BLOCK_RADIATION_RANGE.get());
        json.append(",\"shieldingEnabled\":").append(RadiationConfig.ENABLE_RADIATION_SHIELDING.get());
        json.append(",\"defaultShielding\":").append(RadiationConfig.DEFAULT_BLOCK_SHIELDING.get());
        json.append("}}");

        return json.toString();
    }

    private String getBiomeName(ServerPlayer player) {
        try {
            Biome biome = player.level().getBiome(player.blockPosition()).value();
            ResourceLocation biomeLocation = player.level().registryAccess()
                    .registryOrThrow(Registries.BIOME)
                    .getKey(biome);
            return biomeLocation != null ? biomeLocation.toString() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }

    private float calculateArmorProtection(ServerPlayer player) {
        // Calculate total armor protection from equipped items
        float totalProtection = 0.0f;
        int armorPieces = 0;

        for (net.minecraft.world.item.ItemStack armorItem : player.getArmorSlots()) {
            if (!armorItem.isEmpty()) {
                armorPieces++;
                // Simple calculation based on armor value
                if (armorItem.getItem() instanceof net.minecraft.world.item.ArmorItem armorItemType) {
                    totalProtection += armorItemType.getDefense();
                }
            }
        }

        // Convert to percentage (max armor value is typically 20)
        return Math.min(100.0f, (totalProtection / 20.0f) * 100.0f);
    }

    private String getNearbyRadioactiveBlocksWithShielding(ServerPlayer player) {
        StringBuilder json = new StringBuilder("[");
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();
        int range = RadiationConfig.BLOCK_RADIATION_RANGE.get();
        boolean shieldingEnabled = RadiationConfig.ENABLE_RADIATION_SHIELDING.get();

        // Build a set of radioactive block IDs from the config
        java.util.Set<String> radioactiveBlockIds = new java.util.HashSet<>();
        for (String entry : RadiationConfig.RADIOACTIVE_BLOCKS.get()) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                radioactiveBlockIds.add(parts[0] + ":" + parts[1]);
            }
        }

        boolean first = true;
        int count = 0;

        for (int x = -range; x <= range && count < 20; x++) {
            for (int y = -range; y <= range && count < 20; y++) {
                for (int z = -range; z <= range && count < 20; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = world.getBlockState(checkPos);
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                    String blockIdString = blockId.toString();

                    if (radioactiveBlockIds.contains(blockIdString)) {
                        if (!first)
                            json.append(",");
                        first = false;

                        double distance = Math.sqrt(x * x + y * y + z * z);

                        // Calculate shielding if enabled
                        float shieldingReduction = 0.0f;
                        int blocksBetween = 0;
                        if (shieldingEnabled) {
                            shieldingReduction = DynamicRadiationHandler.calculateShieldingReduction(
                                    player.level(), playerPos, checkPos);
                            blocksBetween = (int) distance;
                        }

                        json.append("{\"block\":\"").append(blockIdString).append("\"");
                        json.append(",\"distance\":").append(String.format(java.util.Locale.US, "%.1f", distance));
                        json.append(",\"pos\":{\"x\":").append(checkPos.getX());
                        json.append(",\"y\":").append(checkPos.getY());
                        json.append(",\"z\":").append(checkPos.getZ()).append("}");
                        json.append(",\"shielding\":")
                                .append(String.format(java.util.Locale.US, "%.1f", shieldingReduction * 100));
                        json.append(",\"isShielded\":").append(shieldingReduction > 0.01f);
                        json.append("}");
                        count++;
                    }
                }
            }
        }

        json.append("]");
        return json.toString();
    }

    private String getNearbyRadioactiveBlocksJson(ServerPlayer player) {
        StringBuilder json = new StringBuilder("[");
        BlockPos playerPos = player.blockPosition();
        Level world = player.level();
        int range = RadiationConfig.BLOCK_RADIATION_RANGE.get();

        // Build a set of radioactive block IDs from the config (format:
        // "modid:block:chance:maxLevel")
        java.util.Set<String> radioactiveBlockIds = new java.util.HashSet<>();
        for (String entry : RadiationConfig.RADIOACTIVE_BLOCKS.get()) {
            String[] parts = entry.split(":");
            if (parts.length >= 2) {
                radioactiveBlockIds.add(parts[0] + ":" + parts[1]);
            }
        }

        boolean first = true;
        int count = 0;

        for (int x = -range; x <= range && count < 20; x++) {
            for (int y = -range; y <= range && count < 20; y++) {
                for (int z = -range; z <= range && count < 20; z++) {
                    BlockPos checkPos = playerPos.offset(x, y, z);
                    BlockState blockState = world.getBlockState(checkPos);
                    ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockState.getBlock());
                    String blockIdString = blockId.toString();

                    if (radioactiveBlockIds.contains(blockIdString)) {
                        if (!first)
                            json.append(",");
                        first = false;

                        double distance = Math.sqrt(x * x + y * y + z * z);
                        json.append("{\"block\":\"").append(blockIdString).append("\"");
                        json.append(",\"distance\":").append(String.format(java.util.Locale.US, "%.1f", distance));
                        json.append(",\"pos\":{\"x\":").append(checkPos.getX());
                        json.append(",\"y\":").append(checkPos.getY());
                        json.append(",\"z\":").append(checkPos.getZ()).append("}}");
                        count++;
                    }
                }
            }
        }

        json.append("]");
        return json.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            // Load from classpath resources
            String resourcePath = "/assets/irradiated/web-ui" + requestPath;
            InputStream is = RadiationDebugServer.class.getResourceAsStream(resourcePath);

            // SPA Fallback: If file not found and not an asset, serve index.html
            if (is == null && !requestPath.startsWith("/assets/")) {
                resourcePath = "/assets/irradiated/web-ui/index.html";
                is = RadiationDebugServer.class.getResourceAsStream(resourcePath);
            }

            if (is == null) {
                send404(exchange);
                return;
            }

            try (InputStream input = is) {
                byte[] bytes = input.readAllBytes();
                String mimeType = getMimeType(requestPath);

                exchange.getResponseHeaders().set("Content-Type", mimeType);
                exchange.getResponseHeaders().set("Connection", "close");
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.flush();
                os.close();
            } catch (Exception e) {
                LOGGER.error("Error serving file: " + requestPath, e);
                sendError(exchange, 500, "Internal Server Error");
            } finally {
                exchange.close();
            }
        }

        private void send404(HttpExchange exchange) throws IOException {
            String response = "404 Not Found (Web UI not found in JAR)";
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        private void sendError(HttpExchange exchange, int code, String message) throws IOException {
            exchange.sendResponseHeaders(code, message.length());
            OutputStream os = exchange.getResponseBody();
            os.write(message.getBytes());
            os.close();
        }

        private String getMimeType(String filename) {
            if (filename.endsWith(".html"))
                return "text/html; charset=UTF-8";
            if (filename.endsWith(".css"))
                return "text/css";
            if (filename.endsWith(".js"))
                return "application/javascript";
            if (filename.endsWith(".json"))
                return "application/json";
            if (filename.endsWith(".png"))
                return "image/png";
            if (filename.endsWith(".jpg"))
                return "image/jpeg";
            if (filename.endsWith(".svg"))
                return "image/svg+xml";
            if (filename.endsWith(".ico"))
                return "image/x-icon";
            return "application/octet-stream";
        }
    }

    private class ConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StringBuilder json = new StringBuilder("{");
                json.append("\"enableEnvironmentalRadiation\":")
                        .append(RadiationConfig.ENABLE_ENVIRONMENTAL_RADIATION.get());
                json.append(",\"enableBiomeRadiation\":").append(RadiationConfig.ENABLE_BIOME_RADIATION.get());
                json.append(",\"enableDimensionRadiation\":").append(RadiationConfig.ENABLE_DIMENSION_RADIATION.get());
                json.append(",\"enableBlockRadiation\":").append(RadiationConfig.ENABLE_BLOCK_RADIATION.get());
                json.append(",\"blockRadiationRange\":").append(RadiationConfig.BLOCK_RADIATION_RANGE.get());
                json.append(",\"enableShielding\":").append(RadiationConfig.ENABLE_RADIATION_SHIELDING.get());
                json.append(",\"defaultShielding\":").append(RadiationConfig.DEFAULT_BLOCK_SHIELDING.get());
                json.append(",\"radiationBuildupRate\":").append(RadiationConfig.RADIATION_BUILDUP_RATE.get());
                json.append(",\"radiationDecayRate\":").append(RadiationConfig.RADIATION_DECAY_RATE.get());
                json.append(",\"radiationDecayDelay\":").append(RadiationConfig.RADIATION_DECAY_DELAY.get());
                json.append("}");

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.getResponseHeaders().set("Connection", "close");
                byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.flush();
                os.close();
            } catch (Exception e) {
                LOGGER.error("Error serving config API", e);
            } finally {
                exchange.close();
            }
        }
    }

    private static class WebSocketClient {
        private final Socket socket;
        private final InputStream input;
        private final OutputStream output;
        private volatile boolean connected = false;

        public WebSocketClient(Socket socket) throws IOException {
            this.socket = socket;
            this.socket.setSoTimeout(60000); // 60 second timeout for reads
            this.socket.setTcpNoDelay(true); // Disable Nagle's algorithm for lower latency
            this.socket.setKeepAlive(true); // Enable TCP keepalive
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
        }

        public boolean performHandshake() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String line;
                String webSocketKey = null;

                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    if (line.startsWith("Sec-WebSocket-Key:")) {
                        webSocketKey = line.substring(19).trim();
                    }
                }

                if (webSocketKey == null) {
                    return false;
                }

                String acceptKey = generateAcceptKey(webSocketKey);
                String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                        "Upgrade: websocket\r\n" +
                        "Connection: Upgrade\r\n" +
                        "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

                output.write(response.getBytes(StandardCharsets.UTF_8));
                output.flush();
                connected = true;
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private String generateAcceptKey(String key) throws Exception {
            String magic = key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(magic.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        }

        public void sendMessage(String message) throws IOException {
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            int length = messageBytes.length;

            ByteArrayOutputStream frame = new ByteArrayOutputStream();
            frame.write(0x81); // Text frame, FIN bit set

            if (length <= 125) {
                frame.write(length);
            } else if (length <= 65535) {
                frame.write(126);
                frame.write((length >> 8) & 0xFF);
                frame.write(length & 0xFF);
            } else {
                frame.write(127);
                for (int i = 7; i >= 0; i--) {
                    frame.write((length >> (8 * i)) & 0xFF);
                }
            }

            frame.write(messageBytes);

            synchronized (output) {
                output.write(frame.toByteArray());
                output.flush();
            }
        }

        public String readMessage() throws IOException {
            try {
                int firstByte = input.read();
                if (firstByte == -1) {
                    connected = false;
                    return null;
                }

                int opcode = firstByte & 0x0F;

                // Handle control frames
                if (opcode == 0x08) { // Close frame
                    connected = false;
                    return null;
                } else if (opcode == 0x09) { // Ping frame
                    handlePingFrame();
                    return null; // Don't return ping as a message
                } else if (opcode == 0x0A) { // Pong frame
                    return null; // Ignore pong frames
                }

                int secondByte = input.read();
                boolean masked = (secondByte & 0x80) != 0;
                int length = secondByte & 0x7F;

                if (length == 126) {
                    length = (input.read() << 8) | input.read();
                } else if (length == 127) {
                    length = 0;
                    for (int i = 0; i < 8; i++) {
                        length = (length << 8) | input.read();
                    }
                }

                byte[] maskKey = null;
                if (masked) {
                    maskKey = new byte[4];
                    input.read(maskKey);
                }

                byte[] payload = new byte[length];
                int read = 0;
                while (read < length) {
                    int r = input.read(payload, read, length - read);
                    if (r == -1)
                        break;
                    read += r;
                }

                if (masked && maskKey != null) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] ^= maskKey[i % 4];
                    }
                }

                return new String(payload, StandardCharsets.UTF_8);
            } catch (java.net.SocketTimeoutException e) {
                // Timeout is normal - just means no data received, connection is still alive
                return null;
            }
        }

        private void handlePingFrame() {
            try {
                // Send pong frame back (opcode 0x0A)
                synchronized (output) {
                    output.write(0x8A); // Pong frame, FIN bit set
                    output.write(0x00); // No payload
                    output.flush();
                }
            } catch (IOException e) {
                connected = false;
            }
        }

        public boolean isConnected() {
            return connected && !socket.isClosed();
        }

        public void close() {
            connected = false;
            try {
                socket.close();
            } catch (IOException e) {
                /* ignore */ }
        }
    }
}
