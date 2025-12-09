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
    
    private RadiationDebugServer() {}
    
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
        
        try {
            // Start HTTP server for dashboard
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            httpServer.createContext("/", new DashboardHandler());
            httpServer.createContext("/api/config", new ConfigApiHandler());
            httpServer.setExecutor(Executors.newFixedThreadPool(2));
            httpServer.start();
            
            // Start WebSocket server on port + 1
            int wsPort = port + 1;
            webSocketExecutor = Executors.newCachedThreadPool();
            webSocketServerSocket = new ServerSocket(wsPort, 50, java.net.InetAddress.getByName("127.0.0.1"));
            LOGGER.info("WebSocket ServerSocket created and bound to 127.0.0.1:{}", wsPort);
            webSocketExecutor.submit(this::acceptWebSocketConnections);
            LOGGER.info("WebSocket acceptor thread submitted");
            
            // Start broadcast scheduler
            scheduler = Executors.newSingleThreadScheduledExecutor();
            int intervalMs = RadiationConfig.DEBUG_UPDATE_INTERVAL_MS.get();
            scheduler.scheduleAtFixedRate(this::broadcastRadiationData, 0, intervalMs, TimeUnit.MILLISECONDS);
            
            running = true;
            LOGGER.warn("===========================================");
            LOGGER.warn("RADIATION DEBUG SERVER STARTED");
            LOGGER.warn("Dashboard: http://localhost:{}/", port);
            LOGGER.warn("WebSocket: ws://localhost:{}/", wsPort);
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
        if (minecraftServer == null) return null;
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
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
    
    private int extractJsonInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0;
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
            if (!first) json.append(",");
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
                        if (!first) json.append(",");
                        first = false;
                        
                        double distance = Math.sqrt(x * x + y * y + z * z);
                        
                        // Calculate shielding if enabled
                        float shieldingReduction = 0.0f;
                        int blocksBetween = 0;
                        if (shieldingEnabled) {
                            shieldingReduction = DynamicRadiationHandler.calculateShieldingReduction(
                                player.level(), playerPos, checkPos
                            );
                            blocksBetween = (int) distance;
                        }
                        
                        json.append("{\"block\":\"").append(blockIdString).append("\"");
                        json.append(",\"distance\":").append(String.format(java.util.Locale.US, "%.1f", distance));
                        json.append(",\"pos\":{\"x\":").append(checkPos.getX());
                        json.append(",\"y\":").append(checkPos.getY());
                        json.append(",\"z\":").append(checkPos.getZ()).append("}");
                        json.append(",\"shielding\":").append(String.format(java.util.Locale.US, "%.1f", shieldingReduction * 100));
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
        
        // Build a set of radioactive block IDs from the config (format: "modid:block:chance:maxLevel")
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
                        if (!first) json.append(",");
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
            try {
                String response = getDashboardHtml();
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Connection", "close");
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.flush();
                os.close();
            } catch (Exception e) {
                LOGGER.error("Error serving dashboard", e);
                String error = "<html><body><h1>Error: " + e.getMessage() + "</h1></body></html>";
                exchange.sendResponseHeaders(500, error.length());
                OutputStream os = exchange.getResponseBody();
                os.write(error.getBytes(StandardCharsets.UTF_8));
                os.close();
            } finally {
                exchange.close();
            }
        }
    }
    
    private class ConfigApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                StringBuilder json = new StringBuilder("{");
                json.append("\"enableEnvironmentalRadiation\":").append(RadiationConfig.ENABLE_ENVIRONMENTAL_RADIATION.get());
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
    
    private String getDashboardHtml() {
        int wsPort = RadiationConfig.DEBUG_SERVER_PORT.get() + 1;
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Irradiated - Radiation Debug Dashboard</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        
        :root {
            --background: 222.2 84% 4.9%;
            --foreground: 210 40% 98%;
            --card: 222.2 84% 4.9%;
            --card-foreground: 210 40% 98%;
            --popover: 222.2 84% 4.9%;
            --popover-foreground: 210 40% 98%;
            --primary: 142.1 76.2% 36.3%;
            --primary-foreground: 355.7 100% 97.3%;
            --secondary: 217.2 32.6% 17.5%;
            --secondary-foreground: 210 40% 98%;
            --muted: 217.2 32.6% 17.5%;
            --muted-foreground: 215 20.2% 65.1%;
            --accent: 217.2 32.6% 17.5%;
            --accent-foreground: 210 40% 98%;
            --destructive: 0 62.8% 30.6%;
            --destructive-foreground: 210 40% 98%;
            --border: 217.2 32.6% 17.5%;
            --input: 217.2 32.6% 17.5%;
            --ring: 142.1 76.2% 36.3%;
            --radius: 0.5rem;
        }
        
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', sans-serif;
            background: hsl(var(--background));
            color: hsl(var(--foreground));
            min-height: 100vh;
            padding: 24px;
            line-height: 1.5;
            -webkit-font-smoothing: antialiased;
        }
        
        /* Header */
        .header {
            position: sticky;
            top: 0;
            z-index: 50;
            text-align: center;
            padding: 24px;
            margin-bottom: 24px;
            background: hsl(var(--card));
            border-radius: var(--radius);
            border: 1px solid hsl(var(--border));
            box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
        }
        
        .header h1 {
            color: hsl(var(--primary));
            font-size: 1.875rem;
            font-weight: 700;
            letter-spacing: -0.025em;
            margin-bottom: 16px;
        }
        
        .header-info {
            display: flex;
            justify-content: center;
            gap: 30px;
            margin-top: 15px;
            flex-wrap: wrap;
        }
        
        .status {
            display: inline-flex;
            align-items: center;
            gap: 8px;
            padding: 6px 16px;
            border-radius: var(--radius);
            font-size: 0.875rem;
            font-weight: 500;
            transition: all 0.2s;
            border: 1px solid;
        }
        
        .status.connected { 
            background: hsl(var(--primary) / 0.1);
            color: hsl(var(--primary));
            border-color: hsl(var(--primary) / 0.3);
        }
        
        .status.disconnected { 
            background: hsl(var(--destructive) / 0.1);
            color: hsl(var(--destructive));
            border-color: hsl(var(--destructive) / 0.3);
        }
        
        .status-dot {
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background: currentColor;
            animation: pulse-dot 2s infinite;
        }
        
        @keyframes pulse-dot {
            0%, 100% { opacity: 1; transform: scale(1); }
            50% { opacity: 0.5; transform: scale(1.2); }
        }
        
        .session-info {
            font-size: 0.9em;
            color: #888;
        }
        
        /* Search and filter bar */
        .filter-bar {
            display: flex;
            gap: 12px;
            padding: 16px;
            background: hsl(var(--card));
            border: 1px solid hsl(var(--border));
            border-radius: var(--radius);
            margin-bottom: 24px;
            flex-wrap: wrap;
            align-items: center;
        }
        
        .filter-bar input, .filter-bar select {
            flex: 1;
            min-width: 150px;
            padding: 8px 12px;
            border: 1px solid hsl(var(--input));
            background: hsl(var(--background));
            color: hsl(var(--foreground));
            border-radius: var(--radius);
            font-size: 0.875rem;
            transition: border-color 0.15s;
        }
        
        .filter-bar input:focus, .filter-bar select:focus {
            outline: none;
            border-color: hsl(var(--ring));
        }
        
        /* Container - modern sidebar layout */
        .container {
            display: grid;
            grid-template-columns: 300px 1fr;
            gap: 24px;
            max-width: 1600px;
            margin: 0 auto;
        }
        
        .sidebar {
            display: flex;
            flex-direction: column;
            gap: 16px;
        }
        
        .main-content {
            display: flex;
            flex-direction: column;
            gap: 24px;
        }
        
        @media (max-width: 1024px) {
            .container {
                grid-template-columns: 1fr;
            }
        }
        
        /* Card styles */
        .card {
            background: hsl(var(--card));
            border-radius: var(--radius);
            padding: 24px;
            border: 1px solid hsl(var(--border));
            box-shadow: 0 1px 3px 0 rgb(0 0 0 / 0.1), 0 1px 2px -1px rgb(0 0 0 / 0.1);
            transition: box-shadow 0.2s;
        }
        
        .card:hover {
            box-shadow: 0 4px 6px -1px rgb(0 0 0 / 0.1), 0 2px 4px -2px rgb(0 0 0 / 0.1);
        }
        
        .card h2 {
            color: hsl(var(--foreground));
            font-size: 1.125rem;
            font-weight: 600;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 1px solid hsl(var(--border));
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        
        .card-badge {
            background: #ff4444;
            color: #fff;
            padding: 4px 10px;
            border-radius: 12px;
            font-size: 0.8em;
            font-weight: bold;
        }
        
        /* Statistics overview */
        .stats-grid {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }
        
        .stat-box {
            display: flex;
            justify-content: space-between;
            align-items: center;
            background: hsl(var(--muted) / 0.3);
            padding: 12px 16px;
            border-radius: var(--radius);
            border: 1px solid hsl(var(--border));
            transition: background 0.15s;
        }
        
        .stat-box:hover {
            background: hsl(var(--muted) / 0.5);
        }
        
        .stat-label-large {
            font-size: 0.875rem;
            color: hsl(var(--muted-foreground));
            font-weight: 500;
        }
        
        .stat-value-large {
            font-size: 1.5rem;
            font-weight: 700;
            color: hsl(var(--primary));
        }
        
        /* Player cards */
        .player-card {
            background: hsl(var(--card));
            border: 1px solid hsl(var(--border));
            padding: 16px;
            margin-bottom: 12px;
            border-radius: var(--radius);
            transition: border-color 0.15s, box-shadow 0.15s;
        }
        
        .player-card.warning {
            border-color: hsl(45 93.4% 47.5%);
            background: hsl(45 93.4% 47.5% / 0.05);
        }
        
        .player-card.danger {
            border-color: hsl(var(--destructive));
            background: hsl(var(--destructive) / 0.05);
        }
        
        .player-card:hover {
            box-shadow: 0 2px 8px rgb(0 0 0 / 0.1);
        }
        
        .player-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;
        }
        
        .player-name {
            font-size: 1.125rem;
            font-weight: 600;
            color: hsl(var(--foreground));
        }
        
        .collapse-btn {
            background: none;
            border: none;
            color: hsl(var(--muted-foreground));
            font-size: 1rem;
            cursor: pointer;
            transition: transform 0.2s, color 0.15s;
            padding: 4px;
        }
        
        .collapse-btn:hover {
            color: hsl(var(--foreground));
        }
        
        .collapse-btn.collapsed {
            transform: rotate(-90deg);
        }
        
        .player-content {
            max-height: 1000px;
            overflow: hidden;
            transition: max-height 0.3s ease-out;
        }
        
        .player-content.collapsed {
            max-height: 0;
        }
        
        /* Progress ring */
        .radiation-progress {
            position: relative;
            width: 100px;
            height: 100px;
            margin: 15px auto;
        }
        
        .radiation-ring {
            transform: rotate(-90deg);
        }
        
        .radiation-ring-bg {
            fill: none;
            stroke: #333;
            stroke-width: 10;
        }
        
        .radiation-ring-progress {
            fill: none;
            stroke-width: 10;
            stroke-linecap: round;
            transition: stroke-dashoffset 0.5s, stroke 0.5s;
        }
        
        .radiation-center {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            text-align: center;
        }
        
        .radiation-value {
            font-size: 1.5em;
            font-weight: bold;
        }
        
        .radiation-label {
            font-size: 0.7em;
            color: #888;
        }
        
        .stat-row {
            display: flex;
            justify-content: space-between;
            padding: 8px 0;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }
        
        .stat-label { color: #888; }
        .stat-value { color: #fff; font-weight: bold; }
        
        /* Controls */
        .controls {
            display: flex;
            gap: 8px;
            margin-top: 15px;
            flex-wrap: wrap;
        }
        
        .controls input {
            width: 70px;
            padding: 8px 12px;
            border: 1px solid hsl(var(--input));
            background: hsl(var(--background));
            color: hsl(var(--foreground));
            border-radius: var(--radius);
            font-size: 0.875rem;
            transition: border-color 0.15s;
        }
        
        .controls input:focus {
            outline: none;
            border-color: hsl(var(--ring));
        }
        
        .controls button {
            padding: 8px 12px;
            border: 1px solid;
            border-radius: var(--radius);
            cursor: pointer;
            font-weight: 500;
            font-size: 0.875rem;
            transition: all 0.15s;
            white-space: nowrap;
        }
        
        .controls button:hover {
            opacity: 0.9;
        }
        
        .controls button:active {
            opacity: 0.8;
        }
        
        .btn-set { 
            background: hsl(var(--primary)); 
            color: hsl(var(--primary-foreground)); 
            border-color: hsl(var(--primary));
        }
        .btn-add { 
            background: hsl(var(--secondary)); 
            color: hsl(var(--secondary-foreground)); 
            border-color: hsl(var(--border));
        }
        .btn-clear { 
            background: hsl(var(--destructive)); 
            color: hsl(var(--destructive-foreground)); 
            border-color: hsl(var(--destructive));
        }
        .btn-copy { 
            background: hsl(var(--secondary)); 
            color: hsl(var(--secondary-foreground)); 
            border-color: hsl(var(--border));
        }
        
        /* Batch operations */
        .batch-ops {
            display: flex;
            gap: 10px;
            flex-wrap: wrap;
            align-items: center;
        }
        
        .batch-ops input {
            width: 80px;
            padding: 10px;
            border: 1px solid #00ff88;
            background: rgba(0,0,0,0.5);
            color: #fff;
            border-radius: 8px;
        }
        
        .batch-ops button {
            padding: 10px 20px;
            border: none;
            border-radius: 8px;
            cursor: pointer;
            font-weight: bold;
            transition: all 0.2s;
        }
        
        .batch-count {
            color: #888;
            font-size: 0.9em;
        }
        
        /* Blocks list */
        .blocks-list {
            max-height: 200px;
            overflow-y: auto;
            font-size: 0.9em;
            margin-top: 10px;
            background: rgba(0,0,0,0.3);
            border-radius: 8px;
            padding: 10px;
        }
        
        .block-item {
            padding: 8px;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
            display: flex;
            justify-content: space-between;
            transition: background 0.2s;
        }
        
        .block-item:hover {
            background: rgba(0, 255, 136, 0.1);
        }
        
        .block-item:last-child {
            border-bottom: none;
        }
        
        .block-distance {
            color: #888;
        }
        
        /* Chart tabs */
        .chart-tabs {
            display: flex;
            gap: 10px;
            margin-bottom: 15px;
        }
        
        .chart-tab {
            padding: 10px 20px;
            background: rgba(0,0,0,0.3);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.2s;
        }
        
        .chart-tab.active {
            background: rgba(0, 255, 136, 0.2);
            border-color: #00ff88;
            color: #00ff88;
        }
        
        .chart-tab:hover {
            background: rgba(0, 255, 136, 0.1);
        }
        
        .chart-container {
            height: 300px;
            position: relative;
        }
        
        /* Alert system */
        .alerts-container {
            max-height: 400px;
            overflow-y: auto;
        }
        
        .alert-item {
            background: rgba(255, 68, 68, 0.1);
            border-left: 4px solid #ff4444;
            padding: 12px;
            margin-bottom: 10px;
            border-radius: 8px;
            display: flex;
            justify-content: space-between;
            align-items: center;
            animation: slideIn 0.3s;
        }
        
        @keyframes slideIn {
            from { transform: translateX(-20px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
        
        .alert-dismiss {
            background: none;
            border: none;
            color: #ff4444;
            cursor: pointer;
            font-size: 1.2em;
            padding: 5px;
        }
        
        /* Toast notifications */
        .toast {
            position: fixed;
            bottom: 20px;
            right: 20px;
            background: rgba(0, 255, 136, 0.95);
            color: #000;
            padding: 15px 25px;
            border-radius: 10px;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
            animation: toastIn 0.3s, toastOut 0.3s 2.7s;
            z-index: 10000;
        }
        
        @keyframes toastIn {
            from { transform: translateX(400px); opacity: 0; }
            to { transform: translateX(0); opacity: 1; }
        }
        
        @keyframes toastOut {
            to { transform: translateX(400px); opacity: 0; }
        }
        
        /* Config items */
        .config-item {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1);
        }
        
        /* Log */
        .log-container {
            max-height: 250px;
            overflow-y: auto;
            font-family: 'Courier New', monospace;
            font-size: 0.85em;
            background: rgba(0,0,0,0.5);
            padding: 10px;
            border-radius: 8px;
        }
        
        .log-entry {
            padding: 4px 0;
            border-bottom: 1px solid rgba(255, 255, 255, 0.05);
        }
        
        /* No players message */
        .no-players {
            text-align: center;
            padding: 60px 20px;
            color: #666;
            font-style: italic;
            font-size: 1.1em;
        }
        
        /* Scrollbar styling */
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }
        
        ::-webkit-scrollbar-track {
            background: rgba(0,0,0,0.3);
            border-radius: 10px;
        }
        
        ::-webkit-scrollbar-thumb {
            background: rgba(0, 255, 136, 0.5);
            border-radius: 10px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: rgba(0, 255, 136, 0.7);
        }
        
        /* Loading skeleton */
        .skeleton {
            background: linear-gradient(90deg, rgba(255,255,255,0.05) 25%, rgba(255,255,255,0.1) 50%, rgba(255,255,255,0.05) 75%);
            background-size: 200% 100%;
            animation: loading 1.5s infinite;
        }
        
        @keyframes loading {
            0% { background-position: 200% 0; }
            100% { background-position: -200% 0; }
        }
        
        /* Modal */
        .modal-overlay {
            position: fixed;
            inset: 0;
            background: rgba(0, 0, 0, 0.8);
            display: none;
            align-items: center;
            justify-content: center;
            z-index: 100;
            backdrop-filter: blur(4px);
            animation: fadeIn 0.15s;
        }
        
        .modal-overlay.show {
            display: flex;
        }
        
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        
        .modal {
            background: hsl(var(--card));
            border: 1px solid hsl(var(--border));
            border-radius: var(--radius);
            padding: 24px;
            max-width: 400px;
            width: 90%;
            box-shadow: 0 20px 25px -5px rgb(0 0 0 / 0.1), 0 8px 10px -6px rgb(0 0 0 / 0.1);
            animation: slideIn 0.2s;
        }
        
        @keyframes slideIn {
            from { transform: translateY(-20px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
        
        .modal-title {
            font-size: 1.125rem;
            font-weight: 600;
            color: hsl(var(--foreground));
            margin-bottom: 12px;
        }
        
        .modal-body {
            color: hsl(var(--muted-foreground));
            margin-bottom: 20px;
            line-height: 1.5;
        }
        
        .modal-actions {
            display: flex;
            gap: 8px;
            justify-content: flex-end;
        }
        
        .modal-actions button {
            padding: 8px 16px;
            border: 1px solid;
            border-radius: var(--radius);
            cursor: pointer;
            font-weight: 500;
            font-size: 0.875rem;
            transition: all 0.15s;
        }
        
        .modal-btn-cancel {
            background: hsl(var(--secondary));
            color: hsl(var(--secondary-foreground));
            border-color: hsl(var(--border));
        }
        
        .modal-btn-confirm {
            background: hsl(var(--primary));
            color: hsl(var(--primary-foreground));
            border-color: hsl(var(--primary));
        }
        
        .modal-btn-danger {
            background: hsl(var(--destructive));
            color: hsl(var(--destructive-foreground));
            border-color: hsl(var(--destructive));
        }
        
        /* Responsive */
        @media (max-width: 1024px) {
            .container {
                grid-template-columns: 1fr;
            }
        }
        
        @media (max-width: 768px) {
            .header h1 {
                font-size: 1.5rem;
            }
            .filter-bar {
                flex-direction: column;
            }
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>☢️ IRRADIATED - Radiation Debug Dashboard</h1>
        <div class="header-info">
            <div id="status" class="status disconnected">
                <span class="status-dot"></span>
                <span>Disconnected</span>
            </div>
            <div id="sessionInfo" class="session-info">Connecting...</div>
        </div>
    </div>
    
    <div class="filter-bar" style="max-width: 1600px; margin: 0 auto 24px;">
        <input type="text" id="searchInput" placeholder="🔍 Search players..." style="flex: 2;">
        <select id="dimensionFilter">
            <option value="">All Dimensions</option>
        </select>
        <select id="sortSelect">
            <option value="name-asc">Name ↑</option>
            <option value="name-desc">Name ↓</option>
            <option value="rad-desc">RAD ↓</option>
            <option value="rad-asc">RAD ↑</option>
        </select>
        <label style="color: hsl(var(--muted-foreground)); display: flex; align-items: center; gap: 8px; min-width: 120px;">
            <span style="font-size: 0.875rem;">Min RAD:</span>
            <input type="range" id="radFilter" min="0" max="100" value="0" style="flex: 1;">
            <span id="radFilterValue" style="font-weight: 600; min-width: 30px;">0</span>
        </label>
    </div>
    
    <div class="container">
        <!-- Sidebar -->
        <div class="sidebar">
            <!-- Statistics -->
            <div class="card">
                <h2>📊 Overview</h2>
                <div class="stats-grid">
                    <div class="stat-box">
                        <span class="stat-label-large">Players</span>
                        <span class="stat-value-large" id="totalPlayers">0</span>
                    </div>
                    <div class="stat-box">
                        <span class="stat-label-large">Avg RAD</span>
                        <span class="stat-value-large" id="avgRadiation">0</span>
                    </div>
                    <div class="stat-box">
                        <span class="stat-label-large">Highest</span>
                        <span class="stat-value-large" id="highestRad" style="font-size: 1rem;">-</span>
                    </div>
                    <div class="stat-box">
                        <span class="stat-label-large">Hot Blocks</span>
                        <span class="stat-value-large" id="totalBlocks">0</span>
                    </div>
                </div>
            </div>
            
            <!-- Alerts -->
            <div class="card" id="alertsCard" style="display: none;">
                <h2>
                    🚨 Alerts
                    <span class="card-badge" id="alertCount">0</span>
                </h2>
                <div class="alerts-container" id="alertsContainer"></div>
            </div>
            
            <!-- Batch Operations -->
            <div class="card">
                <h2>⚡ Quick Actions</h2>
                <div class="batch-ops" style="flex-direction: column;">
                    <div style="display: flex; gap: 8px;">
                        <input type="number" id="batchValue" value="0" min="0" max="100" placeholder="Level" style="flex: 1;">
                        <button class="btn-set" onclick="batchSetAll()">Set All</button>
                    </div>
                    <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                        <button class="btn-add" onclick="batchSet(20)" style="flex: 1;">Low (20)</button>
                        <button class="btn-add" onclick="batchSet(80)" style="flex: 1;">High (80)</button>
                    </div>
                    <div style="display: flex; gap: 8px;">
                        <button class="btn-add" onclick="batchAddAll(10)" style="flex: 1;">+10 All</button>
                        <button class="btn-clear" onclick="batchClearAll()" style="flex: 1;">Clear All</button>
                    </div>
                </div>
                <div class="batch-count" id="batchCount" style="margin-top: 12px; padding-top: 12px; border-top: 1px solid hsl(var(--border));">Affected: 0 players</div>
            </div>
            
            <!-- Config -->
            <div class="card">
                <h2>⚙️ Configuration</h2>
                <div id="config" style="font-size: 0.8125rem;">Loading...</div>
            </div>
            
            <!-- Log -->
            <div class="card">
                <h2>📋 Activity Log</h2>
                <div class="log-container" id="log" style="max-height: 200px;"></div>
            </div>
        </div>
        
        <!-- Main Content -->
        <div class="main-content">
            <!-- Chart -->
            <div class="card">
                <h2>📈 Radiation Analytics</h2>
                <div class="chart-tabs">
                    <div class="chart-tab active" data-chart="line">Over Time</div>
                    <div class="chart-tab" data-chart="bar">Current Levels</div>
                    <div class="chart-tab" data-chart="exposure">Exposure Rate</div>
                </div>
                <div class="chart-container" style="height: 280px;">
                    <canvas id="radiationChart"></canvas>
                </div>
            </div>
            
            <!-- Players -->
            <div class="card">
                <h2>👥 Active Players</h2>
                <div id="players"><div class="no-players">Waiting for data...</div></div>
            </div>
        </div>
    </div>
    
    <!-- Custom Modal -->
    <div class="modal-overlay" id="modalOverlay">
        <div class="modal">
            <div class="modal-title" id="modalTitle">Confirm Action</div>
            <div class="modal-body" id="modalBody">Are you sure?</div>
            <div class="modal-actions">
                <button class="modal-btn-cancel" onclick="closeModal()">Cancel</button>
                <button class="modal-btn-confirm" id="modalConfirm" onclick="confirmModal()">Confirm</button>
            </div>
        </div>
    </div>

    <script>
        const WS_PORT = {{WS_PORT}};
        
        // Global state
        const state = {
            ws: null,
            chart: null,
            currentChartType: 'line',
            players: new Map(),
            playerData: {},
            alerts: [],
            connectionTime: null,
            dataReceived: 0,
            reconnectAttempts: 0,
            settings: {
                maxDataPoints: 60,
                autoScroll: true,
                animations: true,
                alertThreshold: 60
            },
            filters: {
                search: '',
                dimension: '',
                radThreshold: 0,
                sort: 'name-asc'
            }
        };
        
        // Modal functionality
        let modalCallback = null;
        
        function showModal(title, message, confirmText = 'Confirm', isDanger = false) {
            return new Promise((resolve) => {
                document.getElementById('modalTitle').textContent = title;
                document.getElementById('modalBody').textContent = message;
                const confirmBtn = document.getElementById('modalConfirm');
                confirmBtn.textContent = confirmText;
                confirmBtn.className = isDanger ? 'modal-btn-danger' : 'modal-btn-confirm';
                
                modalCallback = resolve;
                document.getElementById('modalOverlay').classList.add('show');
            });
        }
        
        function closeModal() {
            document.getElementById('modalOverlay').classList.remove('show');
            if (modalCallback) {
                modalCallback(false);
                modalCallback = null;
            }
        }
        
        function confirmModal() {
            document.getElementById('modalOverlay').classList.remove('show');
            if (modalCallback) {
                modalCallback(true);
                modalCallback = null;
            }
        }
        
        // Close modal on overlay click
        document.getElementById('modalOverlay').addEventListener('click', (e) => {
            if (e.target.id === 'modalOverlay') {
                closeModal();
            }
        });
        
        // Initialize
        function init() {
            setupEventListeners();
            initChart();
            loadConfig();
            connect();
        }
        
        // Event listeners
        function setupEventListeners() {
            document.getElementById('searchInput').addEventListener('input', (e) => {
                state.filters.search = e.target.value.toLowerCase();
                applyFilters();
            });
            
            document.getElementById('dimensionFilter').addEventListener('change', (e) => {
                state.filters.dimension = e.target.value;
                applyFilters();
            });
            
            document.getElementById('sortSelect').addEventListener('change', (e) => {
                state.filters.sort = e.target.value;
                applyFilters();
            });
            
            document.getElementById('radFilter').addEventListener('input', (e) => {
                const value = e.target.value;
                document.getElementById('radFilterValue').textContent = value;
                state.filters.radThreshold = parseInt(value);
                applyFilters();
            });
            
            // Chart tabs
            document.querySelectorAll('.chart-tab').forEach(tab => {
                tab.addEventListener('click', () => {
                    document.querySelectorAll('.chart-tab').forEach(t => t.classList.remove('active'));
                    tab.classList.add('active');
                    state.currentChartType = tab.dataset.chart;
                    updateChart();
                });
            });
        }
        
        // WebSocket connection
        let keepaliveInterval = null;
        
        function connect() {
            // Clear any existing keepalive
            if (keepaliveInterval) {
                clearInterval(keepaliveInterval);
                keepaliveInterval = null;
            }
            
            state.ws = new WebSocket('ws://localhost:' + WS_PORT);
            
            state.ws.onopen = () => {
                state.connectionTime = Date.now();
                state.reconnectAttempts = 0;
                updateConnectionStatus(true);
                log('Connected to server');
                
                // Start keepalive ping every 30 seconds
                keepaliveInterval = setInterval(() => {
                    if (state.ws && state.ws.readyState === WebSocket.OPEN) {
                        // Send empty ping message
                        try {
                            state.ws.send('');
                        } catch (e) {
                            console.warn('Keepalive ping failed');
                        }
                    }
                }, 30000);
            };
            
            state.ws.onclose = () => {
                if (keepaliveInterval) {
                    clearInterval(keepaliveInterval);
                    keepaliveInterval = null;
                }
                updateConnectionStatus(false);
                log('Disconnected - reconnecting in 3s...');
                state.reconnectAttempts++;
                setTimeout(connect, 3000);
            };
            
            state.ws.onerror = (error) => {
                log('WebSocket error');
                console.error('WebSocket error:', error);
            };
            
            state.ws.onmessage = (event) => {
                try {
                    const data = JSON.parse(event.data);
                    state.dataReceived += event.data.length;
                    updateDashboard(data);
                    updateSessionInfo();
                } catch (e) {
                    log('Error parsing data: ' + e);
                    console.error('Parse error:', e);
                }
            };
        }
        
        // Update connection status
        function updateConnectionStatus(connected) {
            const statusEl = document.getElementById('status');
            if (connected) {
                statusEl.className = 'status connected';
                statusEl.innerHTML = '<span class="status-dot"></span><span>Connected</span>';
            } else {
                statusEl.className = 'status disconnected';
                statusEl.innerHTML = '<span class="status-dot"></span><span>Disconnected</span>';
            }
        }
        
        // Update session info
        function updateSessionInfo() {
            if (!state.connectionTime) return;
            const uptime = Math.floor((Date.now() - state.connectionTime) / 1000);
            const minutes = Math.floor(uptime / 60);
            const seconds = uptime % 60;
            const dataKB = (state.dataReceived / 1024).toFixed(1);
            document.getElementById('sessionInfo').textContent = 
                `Uptime: ${minutes}m ${seconds}s | Data: ${dataKB} KB | Reconnects: ${state.reconnectAttempts}`;
        }
        
        // Main dashboard update (incremental DOM updates)
        function updateDashboard(data) {
            const playersDiv = document.getElementById('players');
            
            if (!data.players || data.players.length === 0) {
                playersDiv.innerHTML = '<div class="no-players">No players online</div>';
                updateStatistics(data);
                return;
            }
            
            // Clear "waiting for data" message if it exists
            const noPlayersMsg = playersDiv.querySelector('.no-players');
            if (noPlayersMsg) {
                noPlayersMsg.remove();
            }
            
            const currentPlayerNames = new Set();
            
            // Update dimension filter
            updateDimensionFilter(data.players);
            
            // Process each player
            data.players.forEach(player => {
                currentPlayerNames.add(player.name);
                
                let card = playersDiv.querySelector(`[data-player="${player.name}"]`);
                if (!card) {
                    card = createPlayerCard(player);
                    playersDiv.appendChild(card);
                } else {
                    updatePlayerData(card, player);
                }
                
                // Track history for chart
                if (!state.playerData[player.name]) {
                    state.playerData[player.name] = { times: [], values: [], exposure: [] };
                }
                const pd = state.playerData[player.name];
                pd.times.push(new Date().toLocaleTimeString());
                pd.values.push(player.radiationLevel);
                pd.exposure.push(player.dynamicExposure);
                if (pd.times.length > state.settings.maxDataPoints) {
                    pd.times.shift();
                    pd.values.shift();
                    pd.exposure.shift();
                }
                
                // Check for alerts
                if (player.radiationLevel >= state.settings.alertThreshold) {
                    addAlert(player.name, player.radiationLevel);
                }
            });
            
            // Remove cards for players who left
            playersDiv.querySelectorAll('.player-card').forEach(card => {
                const playerName = card.dataset.player;
                if (!currentPlayerNames.has(playerName)) {
                    card.remove();
                }
            });
            
            updateStatistics(data);
            updateChart();
            applyFilters();
        }
        
        // Create player card (called once per player)
        function createPlayerCard(player) {
            const card = document.createElement('div');
            card.className = 'player-card';
            card.dataset.player = player.name;
            card.dataset.dimension = player.dimension;
            card.dataset.radiation = player.radiationLevel;
            
            card.innerHTML = `
                <div class="player-header">
                    <div class="player-name">👤 <span class="player-name-text">${player.name}</span></div>
                    <button class="collapse-btn" onclick="toggleCollapse('${player.name}')">▼</button>
                </div>
                <div class="player-content" data-content="${player.name}">
                    <div class="radiation-progress">
                        <svg class="radiation-ring" width="100" height="100">
                            <circle class="radiation-ring-bg" cx="50" cy="50" r="40"></circle>
                            <circle class="radiation-ring-progress" cx="50" cy="50" r="40" 
                                    stroke-dasharray="251.2" stroke-dashoffset="251.2"></circle>
                        </svg>
                        <div class="radiation-center">
                            <div class="radiation-value">${player.radiationLevel}</div>
                            <div class="radiation-label">RAD</div>
                        </div>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Dynamic Exposure</span>
                        <span class="stat-value" data-field="exposure">${player.dynamicExposure}</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Position</span>
                        <span class="stat-value" data-field="position">${player.position.x}, ${player.position.y}, ${player.position.z}</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Dimension</span>
                        <span class="stat-value" data-field="dimension">${player.dimension}</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Biome</span>
                        <span class="stat-value" data-field="biome">${player.biome}</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Rad Resistance</span>
                        <span class="stat-value" data-field="resistance">${player.hasRadResistance ? `✅ Level ${player.radResistanceLevel} (${formatDuration(player.radResistanceDuration)})` : '❌ No'}</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Armor Protection</span>
                        <span class="stat-value" data-field="armor">🛡️ ${player.armorProtection}%</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">In Water</span>
                        <span class="stat-value" data-field="water">${player.isInWater ? '✅ Yes' : '❌ No'}</span>
                    </div>
                    <div class="stat-row">
                        <span class="stat-label">Nearby Radioactive Blocks</span>
                        <span class="stat-value" data-field="blockCount">${player.nearbyRadioactiveBlocks.length}</span>
                    </div>
                    <div class="blocks-list" data-field="blocks" style="display: ${player.nearbyRadioactiveBlocks.length > 0 ? 'block' : 'none'}">
                        ${renderBlocks(player.nearbyRadioactiveBlocks)}
                    </div>
                    <div class="controls">
                        <input type="number" data-input="${player.name}" value="${player.radiationLevel}" min="0" max="100">
                        <button class="btn-set" onclick="setRad('${player.name}')">Set</button>
                        <button class="btn-add" onclick="addRad('${player.name}', 10)">+10</button>
                        <button class="btn-add" onclick="addRad('${player.name}', 25)">+25</button>
                        <button class="btn-clear" onclick="clearRad('${player.name}')">Clear</button>
                        <button class="btn-copy" onclick="copyCoords('${player.name}')">📋 Coords</button>
                    </div>
                </div>
            `;
            
            return card;
        }
        
        // Update player data (called every update)
        function updatePlayerData(card, player) {
            card.dataset.radiation = player.radiationLevel;
            card.dataset.dimension = player.dimension;
            
            // Update severity class
            card.classList.remove('warning', 'danger');
            if (player.radiationLevel >= 80) {
                card.classList.add('danger');
            } else if (player.radiationLevel >= 60) {
                card.classList.add('warning');
            }
            
            // Update progress ring
            const progress = card.querySelector('.radiation-ring-progress');
            const value = card.querySelector('.radiation-value');
            const circumference = 251.2;
            const offset = circumference - (player.radiationLevel / 100) * circumference;
            progress.style.strokeDashoffset = offset;
            progress.style.stroke = getRadiationColor(player.radiationLevel);
            value.textContent = player.radiationLevel;
            value.style.color = getRadiationColor(player.radiationLevel);
            
            // Update data fields
            card.querySelector('[data-field="exposure"]').textContent = player.dynamicExposure;
            card.querySelector('[data-field="position"]').textContent = 
                `${player.position.x}, ${player.position.y}, ${player.position.z}`;
            card.querySelector('[data-field="dimension"]').textContent = player.dimension;
            card.querySelector('[data-field="biome"]').textContent = player.biome;
            card.querySelector('[data-field="resistance"]').textContent = player.hasRadResistance 
                ? `✅ Level ${player.radResistanceLevel} (${formatDuration(player.radResistanceDuration)})` 
                : '❌ No';
            card.querySelector('[data-field="armor"]').textContent = `🛡️ ${player.armorProtection}%`;
            card.querySelector('[data-field="water"]').textContent = player.isInWater ? '✅ Yes' : '❌ No';
            card.querySelector('[data-field="blockCount"]').textContent = player.nearbyRadioactiveBlocks.length;
            
            // Update blocks list (without innerHTML to prevent twitching)
            const blocksList = card.querySelector('[data-field="blocks"]');
            const blocksKey = JSON.stringify(player.nearbyRadioactiveBlocks.map(b => b.block + b.distance));
            const currentKey = blocksList.dataset.blocksKey;
            
            if (player.nearbyRadioactiveBlocks.length > 0) {
                blocksList.style.display = 'block';
                // Only update if blocks changed
                if (blocksKey !== currentKey) {
                    blocksList.dataset.blocksKey = blocksKey;
                    blocksList.innerHTML = renderBlocks(player.nearbyRadioactiveBlocks);
                }
            } else {
                blocksList.style.display = 'none';
                blocksList.dataset.blocksKey = '';
            }
            
            // Update input value (NEVER touch if user is typing or focused)
            const input = card.querySelector(`[data-input="${player.name}"]`);
            if (input && document.activeElement !== input && !input.matches(':focus')) {
                // Only update if value actually changed
                if (parseInt(input.value) !== player.radiationLevel) {
                    input.value = player.radiationLevel;
                }
            }
        }
        
        // Format duration in ticks to readable time
        function formatDuration(ticks) {
            if (ticks <= 0) return '0s';
            const seconds = Math.floor(ticks / 20);
            if (seconds < 60) return `${seconds}s`;
            const minutes = Math.floor(seconds / 60);
            const secs = seconds % 60;
            return `${minutes}m ${secs}s`;
        }
        
        // Render blocks with shielding information
        function renderBlocks(blocks) {
            const grouped = {};
            blocks.forEach(b => {
                if (!grouped[b.block]) {
                    grouped[b.block] = { count: 0, minDist: 999, closest: b };
                }
                grouped[b.block].count++;
                if (b.distance < grouped[b.block].minDist) {
                    grouped[b.block].minDist = b.distance;
                    grouped[b.block].closest = b;
                }
            });
            
            return Object.entries(grouped).map(([block, info]) => {
                const b = info.closest;
                const distColor = b.distance < 3 ? '#ff4444' : b.distance < 6 ? '#ff8800' : '#888';
                const shieldingBadge = b.isShielded 
                    ? `<span style="background: hsl(var(--primary) / 0.15); color: hsl(var(--primary)); padding: 2px 6px; border-radius: 4px; font-size: 0.75rem; margin-left: 6px;">🛡️ ${b.shielding.toFixed(0)}%</span>` 
                    : '';
                const blockShortName = block.split(':')[1] || block;
                return `
                    <div class="block-item">
                        <div style="display: flex; align-items: center; gap: 4px; flex: 1;">
                            <span style="font-weight: 500;">${blockShortName}</span>
                            ${info.count > 1 ? `<span style="color: hsl(var(--muted-foreground)); font-size: 0.8125rem;">x${info.count}</span>` : ''}
                            ${shieldingBadge}
                        </div>
                        <span class="block-distance" style="color: ${distColor}; font-size: 0.75rem;">
                            ${b.distance}m
                        </span>
                    </div>
                `;
            }).join('');
        }
        
        // Toggle collapse
        function toggleCollapse(playerName) {
            const card = document.querySelector(`[data-player="${playerName}"]`);
            const content = card.querySelector('[data-content]');
            const btn = card.querySelector('.collapse-btn');
            content.classList.toggle('collapsed');
            btn.classList.toggle('collapsed');
        }
        
        // Update statistics
        function updateStatistics(data) {
            if (!data.players || data.players.length === 0) {
                document.getElementById('totalPlayers').textContent = '0';
                document.getElementById('avgRadiation').textContent = '0';
                document.getElementById('highestRad').textContent = '-';
                document.getElementById('totalBlocks').textContent = '0';
                document.getElementById('batchCount').textContent = 'Affected: 0 players';
                return;
            }
            
            const totalRad = data.players.reduce((sum, p) => sum + p.radiationLevel, 0);
            const avgRad = Math.round(totalRad / data.players.length);
            const highest = data.players.reduce((max, p) => p.radiationLevel > max.radiationLevel ? p : max);
            const totalBlocks = data.players.reduce((sum, p) => sum + p.nearbyRadioactiveBlocks.length, 0);
            
            document.getElementById('totalPlayers').textContent = data.players.length;
            document.getElementById('avgRadiation').textContent = avgRad;
            document.getElementById('highestRad').textContent = highest.name.length > 12 ? highest.name.substring(0, 12) + '...' : highest.name;
            document.getElementById('totalBlocks').textContent = totalBlocks;
            document.getElementById('batchCount').textContent = `Affected: ${data.players.length} player${data.players.length !== 1 ? 's' : ''}`;
        }
        
        // Add alert
        function addAlert(playerName, level) {
            const alertKey = `${playerName}-${level}`;
            if (state.alerts.includes(alertKey)) return;
            
            state.alerts.push(alertKey);
            const alertsCard = document.getElementById('alertsCard');
            const alertsContainer = document.getElementById('alertsContainer');
            const alertCount = document.getElementById('alertCount');
            
            alertsCard.style.display = 'block';
            alertCount.textContent = state.alerts.length;
            
            const alertEl = document.createElement('div');
            alertEl.className = 'alert-item';
            alertEl.innerHTML = `
                <div>
                    <strong>${playerName}</strong> has reached ${level} radiation!
                    <div style="font-size: 0.85em; color: #888; margin-top: 4px;">
                        ${new Date().toLocaleTimeString()}
                    </div>
                </div>
                <button class="alert-dismiss" onclick="dismissAlert(this, '${alertKey}')">✕</button>
            `;
            alertsContainer.insertBefore(alertEl, alertsContainer.firstChild);
        }
        
        // Dismiss alert
        function dismissAlert(btn, alertKey) {
            state.alerts = state.alerts.filter(a => a !== alertKey);
            btn.parentElement.remove();
            document.getElementById('alertCount').textContent = state.alerts.length;
            if (state.alerts.length === 0) {
                document.getElementById('alertsCard').style.display = 'none';
            }
        }
        
        // Apply filters
        function applyFilters() {
            const cards = document.querySelectorAll('.player-card');
            const filtered = [];
            
            cards.forEach(card => {
                const playerName = card.dataset.player.toLowerCase();
                const dimension = card.dataset.dimension;
                const radiation = parseInt(card.dataset.radiation);
                
                let show = true;
                
                if (state.filters.search && !playerName.includes(state.filters.search)) {
                    show = false;
                }
                
                if (state.filters.dimension && dimension !== state.filters.dimension) {
                    show = false;
                }
                
                if (radiation < state.filters.radThreshold) {
                    show = false;
                }
                
                card.style.display = show ? 'block' : 'none';
                if (show) filtered.push(card);
            });
            
            // Apply sorting
            sortCards(filtered);
        }
        
        // Sort cards
        function sortCards(cards) {
            const [field, order] = state.filters.sort.split('-');
            
            cards.sort((a, b) => {
                let valA, valB;
                if (field === 'name') {
                    valA = a.dataset.player;
                    valB = b.dataset.player;
                } else if (field === 'rad') {
                    valA = parseInt(a.dataset.radiation);
                    valB = parseInt(b.dataset.radiation);
                }
                
                if (order === 'asc') {
                    return valA > valB ? 1 : -1;
                } else {
                    return valA < valB ? 1 : -1;
                }
            });
            
            const playersDiv = document.getElementById('players');
            cards.forEach(card => playersDiv.appendChild(card));
        }
        
        // Update dimension filter
        function updateDimensionFilter(players) {
            const dimensions = new Set(players.map(p => p.dimension));
            const select = document.getElementById('dimensionFilter');
            const currentValue = select.value;
            
            select.innerHTML = '<option value="">All Dimensions</option>';
            dimensions.forEach(dim => {
                const option = document.createElement('option');
                option.value = dim;
                option.textContent = dim.split(':')[1] || dim;
                select.appendChild(option);
            });
            
            if (dimensions.has(currentValue)) {
                select.value = currentValue;
            }
        }
        
        // Get radiation color
        function getRadiationColor(level) {
            if (level < 20) return '#00ff88';
            if (level < 40) return '#88ff00';
            if (level < 60) return '#ffff00';
            if (level < 80) return '#ff8800';
            return '#ff4444';
        }
        
        // Player actions
        function setRad(name) {
            const input = document.querySelector(`[data-input="${name}"]`);
            const level = parseInt(input.value) || 0;
            state.ws.send(JSON.stringify({ action: 'set', player: name, level: level }));
            log(`Set ${name} radiation to ${level}`);
            showToast(`Set ${name} to ${level}`);
        }
        
        function addRad(name, amount) {
            state.ws.send(JSON.stringify({ action: 'add', player: name, amount: amount }));
            log(`Added ${amount} radiation to ${name}`);
            showToast(`Added ${amount} to ${name}`);
        }
        
        function clearRad(name) {
            state.ws.send(JSON.stringify({ action: 'clear', player: name }));
            log(`Cleared radiation for ${name}`);
            showToast(`Cleared ${name}`);
        }
        
        function copyCoords(name) {
            const card = document.querySelector(`[data-player="${name}"]`);
            const coords = card.querySelector('[data-field="position"]').textContent;
            navigator.clipboard.writeText(coords);
            showToast(`Copied coordinates: ${coords}`);
        }
        
        // Batch operations
        async function batchSetAll() {
            const level = parseInt(document.getElementById('batchValue').value) || 0;
            const confirmed = await showModal(
                'Set All Players',
                `Set all players to ${level} radiation?`,
                'Set All',
                false
            );
            if (!confirmed) return;
            
            document.querySelectorAll('.player-card').forEach(card => {
                if (card.style.display !== 'none') {
                    const name = card.dataset.player;
                    state.ws.send(JSON.stringify({ action: 'set', player: name, level: level }));
                }
            });
            log(`Batch set all to ${level}`);
            showToast(`Set all players to ${level}`);
        }
        
        async function batchSet(level) {
            const confirmed = await showModal(
                'Set All Players',
                `Set all players to ${level} radiation?`,
                'Set All',
                false
            );
            if (!confirmed) return;
            
            document.querySelectorAll('.player-card').forEach(card => {
                if (card.style.display !== 'none') {
                    const name = card.dataset.player;
                    state.ws.send(JSON.stringify({ action: 'set', player: name, level: level }));
                }
            });
            log(`Batch set all to ${level}`);
            showToast(`Set all players to ${level}`);
        }
        
        async function batchClearAll() {
            const confirmed = await showModal(
                'Clear All Radiation',
                'Clear radiation for all players?',
                'Clear All',
                true
            );
            if (!confirmed) return;
            
            document.querySelectorAll('.player-card').forEach(card => {
                if (card.style.display !== 'none') {
                    const name = card.dataset.player;
                    state.ws.send(JSON.stringify({ action: 'clear', player: name }));
                }
            });
            log('Batch cleared all players');
            showToast('Cleared all players');
        }
        
        async function batchAddAll(amount) {
            const confirmed = await showModal(
                'Add Radiation',
                `Add ${amount} radiation to all players?`,
                'Add to All',
                false
            );
            if (!confirmed) return;
            
            document.querySelectorAll('.player-card').forEach(card => {
                if (card.style.display !== 'none') {
                    const name = card.dataset.player;
                    state.ws.send(JSON.stringify({ action: 'add', player: name, amount: amount }));
                }
            });
            log(`Batch added ${amount} to all`);
            showToast(`Added ${amount} to all players`);
        }
        
        // Toast notification
        function showToast(message) {
            const toast = document.createElement('div');
            toast.className = 'toast';
            toast.textContent = message;
            document.body.appendChild(toast);
            setTimeout(() => toast.remove(), 3000);
        }
        
        // Log
        function log(msg) {
            const logDiv = document.getElementById('log');
            const time = new Date().toLocaleTimeString();
            const entry = document.createElement('div');
            entry.className = 'log-entry';
            entry.textContent = `[${time}] ${msg}`;
            logDiv.insertBefore(entry, logDiv.firstChild);
            if (logDiv.children.length > 100) {
                logDiv.removeChild(logDiv.lastChild);
            }
        }
        
        // Chart initialization
        function initChart() {
            const ctx = document.getElementById('radiationChart').getContext('2d');
            state.chart = new Chart(ctx, {
                type: 'line',
                data: { labels: [], datasets: [] },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    animation: { duration: 300 },
                    scales: {
                        y: { 
                            beginAtZero: true, 
                            max: 100, 
                            grid: { color: 'rgba(255,255,255,0.1)' }, 
                            ticks: { color: '#888' } 
                        },
                        x: { 
                            grid: { color: 'rgba(255,255,255,0.1)' }, 
                            ticks: { color: '#888', maxTicksLimit: 10 } 
                        }
                    },
                    plugins: { 
                        legend: { labels: { color: '#888' } },
                        tooltip: {
                            backgroundColor: 'rgba(0,0,0,0.8)',
                            borderColor: '#00ff88',
                            borderWidth: 1
                        }
                    }
                }
            });
        }
        
        // Chart update
        function updateChart() {
            const colors = ['#00ff88', '#ff8800', '#8888ff', '#ff88ff', '#ffff00', '#00ffff'];
            let i = 0;
            
            if (state.currentChartType === 'line') {
                state.chart.config.type = 'line';
                state.chart.data.datasets = [];
                for (const [name, data] of Object.entries(state.playerData)) {
                    state.chart.data.labels = data.times;
                    state.chart.data.datasets.push({
                        label: name,
                        data: data.values,
                        borderColor: colors[i % colors.length],
                        backgroundColor: colors[i % colors.length] + '33',
                        fill: false,
                        tension: 0.4
                    });
                    i++;
                }
            } else if (state.currentChartType === 'bar') {
                state.chart.config.type = 'bar';
                const names = [];
                const values = [];
                for (const [name, data] of Object.entries(state.playerData)) {
                    names.push(name);
                    values.push(data.values[data.values.length - 1] || 0);
                }
                state.chart.data.labels = names;
                state.chart.data.datasets = [{
                    label: 'Current Radiation',
                    data: values,
                    backgroundColor: values.map(v => getRadiationColor(v) + 'aa'),
                    borderColor: values.map(v => getRadiationColor(v)),
                    borderWidth: 2
                }];
            } else if (state.currentChartType === 'exposure') {
                state.chart.config.type = 'line';
                state.chart.data.datasets = [];
                for (const [name, data] of Object.entries(state.playerData)) {
                    state.chart.data.labels = data.times;
                    state.chart.data.datasets.push({
                        label: name + ' (Exposure)',
                        data: data.exposure,
                        borderColor: colors[i % colors.length],
                        backgroundColor: colors[i % colors.length] + '33',
                        fill: true,
                        tension: 0.4
                    });
                    i++;
                }
            }
            
            state.chart.update('none');
        }
        
        // Load config
        async function loadConfig() {
            try {
                const res = await fetch('/api/config');
                const config = await res.json();
                let html = '';
                for (const [key, value] of Object.entries(config)) {
                    html += `<div class="config-item"><span>${key}</span><span style="color: #00ff88">${value}</span></div>`;
                }
                document.getElementById('config').innerHTML = html;
            } catch (e) {
                document.getElementById('config').innerHTML = '<div style="color: #ff4444">Failed to load config</div>';
            }
        }
        
        // Start everything
        init();
    </script>
</body>
</html>
""".replace("{{WS_PORT}}", String.valueOf(wsPort));
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
                    if (r == -1) break;
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
            try { socket.close(); } catch (IOException e) { /* ignore */ }
        }
    }
}
