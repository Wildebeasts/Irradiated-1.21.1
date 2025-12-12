import { useEffect, useState, useRef } from 'react';

export interface Position {
    x: number;
    y: number;
    z: number;
}

export interface PlayerData {
    name: string;
    uuid: string;
    position: Position;
    dimension: string;
    biome: string;
    radiationLevel: number;
    dynamicExposure: number;
    hasRadResistance: boolean;
    radResistanceLevel: number;
    radResistanceDuration: number;
    isInWater: boolean;
    armorProtection: number;
    nearbyRadioactiveBlocks: any[];
}

export interface ServerConfig {
    blockRadiationRange: number;
    shieldingEnabled: boolean;
    defaultShielding: number;
}

export interface RadiationData {
    timestamp: number;
    players: PlayerData[];
    config: ServerConfig;
}

export function useRadiationData() {
    const [data, setData] = useState<RadiationData | null>(null);
    const [status, setStatus] = useState<'connected' | 'disconnected' | 'connecting'>('disconnected');
    const ws = useRef<WebSocket | null>(null);

    useEffect(() => {
        let reconnectTimeout: NodeJS.Timeout;

        const connect = () => {
            // Determine WebSocket URL
            // If dev mode (port 5173), assume server is on localhost:DEBUG_PORT (default 8001 or inferred)
            // But we don't know the port for sure.
            // In PROD (served by Java), it will be on the same host:port+1 or similar?
            // Actually Java server says: 
            // Dashboard: http://localhost:port/
            // WebSocket: ws://localhost:wsPort/
            // For now, let's hardcode localhost:8001 for dev, or derive from window.location for prod.
            
            // Logic to detect if we are running in dev or prod served by the mod
            // If served by mod, window.location.port is the HTTP port. optional WS port usually +1
            // But we can try to guess or let user configure.
            // For dev, defaults to localhost:8001 (based on common defaults).
            
            const isDev = import.meta.env.DEV;
            const host = window.location.hostname;
            // Default mod ports: HTTP 8000, WS 8001?
            // The code says "wsPort = port + 1".
            // We'll try to guess port.
            let wsUrl = `ws://${host}:8001`; 
            
            if (isDev) {
                wsUrl = "ws://localhost:8001";
            } else {
                 // In production, served by Java HTTP server on port X. WS is likely X+1.
                 const currentPort = parseInt(window.location.port || "80");
                 wsUrl = `ws://${host}:${currentPort + 1}`;
            }

            setStatus('connecting');
            const socket = new WebSocket(wsUrl);

            socket.onopen = () => {
                setStatus('connected');
            };

            socket.onmessage = (event) => {
                try {
                    const parsed = JSON.parse(event.data);
                    setData(parsed);
                } catch (e) {
                    console.error("Failed to parse WS data", e);
                }
            };

            socket.onclose = () => {
                setStatus('disconnected');
                reconnectTimeout = setTimeout(connect, 3000);
            };

            ws.current = socket;
        };

        connect();

        return () => {
            if (ws.current) {
                ws.current.close();
            }
            clearTimeout(reconnectTimeout);
        };
    }, []);

    const sendCommand = (cmd: any) => {
        if (ws.current && ws.current.readyState === WebSocket.OPEN) {
            ws.current.send(JSON.stringify(cmd));
        }
    };

    return { data, status, sendCommand };
}
