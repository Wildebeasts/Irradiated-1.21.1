
import { useState } from "react"
import { useRadiationData } from "@/hooks/useRadiationData"
import { PlayerTable } from "./PlayerTable"
import { Overview } from "./Overview"
import { QuickActions } from "./QuickActions"
import { ConfigPanel } from "./ConfigPanel"
import { RadiationChart } from "./RadiationChart"

import { Input } from "@/components/ui/input"
import { Slider } from "@/components/ui/slider"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { RadioReceiver, Search, Radio, Wifi, WifiOff, Database, Clock } from "lucide-react"
import { motion } from "framer-motion"

export function Dashboard() {
    const { data, status, sendCommand } = useRadiationData();
    const [searchTerm, setSearchTerm] = useState("");
    const [dimensionFilter, setDimensionFilter] = useState("all");
    const [minRad, setMinRad] = useState([0]);
    const [currentTime, setCurrentTime] = useState(new Date().toLocaleTimeString());

    // Update time every second
    useState(() => {
        const interval = setInterval(() => {
            setCurrentTime(new Date().toLocaleTimeString());
        }, 1000);
        return () => clearInterval(interval);
    });

    if (!data && status !== 'connected') {
        return (
            <div className="min-h-screen bg-black bg-gradient-mesh flex flex-col items-center justify-center gap-8">
                {/* Animated loading state */}
                <motion.div
                    initial={{ scale: 0.8, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    transition={{ duration: 0.6, ease: "easeOut" }}
                    className="relative"
                >
                    <div className="absolute inset-0 bg-primary/20 blur-3xl rounded-full" />
                    <Radio className="h-24 w-24 text-primary relative z-10 animate-pulse" />
                </motion.div>
                
                <motion.div
                    initial={{ y: 20, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    transition={{ delay: 0.2, duration: 0.5 }}
                    className="text-center space-y-4"
                >
                    <h1 className="text-5xl md:text-6xl font-bold tracking-tighter bg-gradient-to-r from-primary via-green-400 to-emerald-600 bg-clip-text text-transparent">
                        IRRADIATED
                    </h1>
                    <p className="text-muted-foreground text-lg font-mono">Radiation Debug Console</p>
                </motion.div>

                <motion.div 
                    initial={{ y: 20, opacity: 0 }}
                    animate={{ y: 0, opacity: 1 }}
                    transition={{ delay: 0.4, duration: 0.5 }}
                    className="flex items-center gap-3 glass px-6 py-3 rounded-full"
                >
                    <div className="h-3 w-3 rounded-full bg-primary pulse-ring" />
                    <span className="text-muted-foreground font-mono text-sm">
                        {status === 'connecting' ? 'Establishing connection...' : 'Waiting for server...'}
                    </span>
                </motion.div>
            </div>
        )
    }

    const allPlayers = data?.players || [];
    
    // Extract unique dimensions for filter
    const dimensions = Array.from(new Set(allPlayers.map(p => p.dimension)));

    // Filter players
    const filteredPlayers = allPlayers.filter(p => {
        const matchesSearch = p.name.toLowerCase().includes(searchTerm.toLowerCase());
        const matchesDimension = dimensionFilter === "all" || p.dimension === dimensionFilter;
        const matchesRad = p.radiationLevel >= minRad[0];
        return matchesSearch && matchesDimension && matchesRad;
    });

    return (
        <div className="min-h-screen bg-black bg-gradient-mesh text-foreground custom-scrollbar">
            {/* Header */}
            <header className="glass-strong border-b border-white/5 sticky top-0 z-50">
                <div className="w-full flex h-16 items-center justify-between px-6 lg:px-10">
                    <div className="flex items-center gap-4">
                        <div className="relative">
                            <div className="absolute inset-0 bg-primary/30 blur-xl rounded-full" />
                            <RadioReceiver className="h-7 w-7 text-primary relative z-10 animate-pulse" />
                        </div>
                        <div>
                            <h1 className="text-xl font-bold bg-gradient-to-r from-primary via-green-400 to-emerald-500 bg-clip-text text-transparent">
                                IRRADIATED
                            </h1>
                            <p className="text-xs text-muted-foreground hidden sm:block">Radiation Debug Dashboard</p>
                        </div>
                    </div>

                    <div className="flex items-center gap-6">
                        {/* Live clock */}
                        <div className="hidden lg:flex items-center gap-2 text-muted-foreground">
                            <Clock className="h-4 w-4" />
                            <span className="font-mono text-sm">{currentTime}</span>
                        </div>

                        {/* Data size */}
                        <div className="hidden md:flex items-center gap-2 text-muted-foreground">
                            <Database className="h-4 w-4" />
                            <span className="font-mono text-sm">{(JSON.stringify(data).length / 1024).toFixed(1)} KB</span>
                        </div>
                        
                        {/* Connection status */}
                        <motion.div 
                            initial={{ scale: 0.9 }}
                            animate={{ scale: 1 }}
                            className={`flex items-center gap-2 px-4 py-1.5 rounded-full ${
                                status === 'connected' 
                                    ? 'bg-primary/10 border border-primary/30' 
                                    : 'bg-red-500/10 border border-red-500/30'
                            }`}
                        >
                            {status === 'connected' ? (
                                <Wifi className="h-4 w-4 text-primary" />
                            ) : (
                                <WifiOff className="h-4 w-4 text-red-500" />
                            )}
                            <span className={`font-mono text-sm ${status === 'connected' ? 'text-primary' : 'text-red-500'}`}>
                                {status === 'connected' ? 'Live' : 'Offline'}
                            </span>
                            {status === 'connected' && (
                                <div className="h-2 w-2 rounded-full bg-primary pulse-ring" />
                            )}
                        </motion.div>
                    </div>
                </div>
            </header>

            <motion.main 
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="w-full px-4 lg:px-8 xl:px-12 py-6 space-y-6"
            >
                {/* Filters Bar */}
                <motion.div 
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ delay: 0.1, duration: 0.4 }}
                    className="grid gap-4 md:grid-cols-4 lg:grid-cols-6 items-center glass p-5 rounded-xl"
                >
                    <div className="md:col-span-2 relative">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input 
                            placeholder="Search players..." 
                            className="pl-10 bg-background/30 border-white/10 focus:border-primary/50 focus:ring-primary/20 transition-all"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                    </div>
                    
                    <Select value={dimensionFilter} onValueChange={setDimensionFilter}>
                        <SelectTrigger className="bg-background/30 border-white/10 focus:border-primary/50">
                            <SelectValue placeholder="All Dimensions" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">All Dimensions</SelectItem>
                            {dimensions.map(d => (
                                <SelectItem key={d} value={d}>{d}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>

                    <div className="md:col-span-1 lg:col-span-2 flex items-center gap-4 px-2">
                        <span className="text-sm text-muted-foreground whitespace-nowrap font-mono">
                            MIN: <span className="text-primary font-semibold">{minRad[0]}</span>
                        </span>
                        <Slider 
                            value={minRad} 
                            onValueChange={setMinRad} 
                            max={100} 
                            step={5}
                            className="w-full"
                        />
                    </div>

                    {/* Player count badge */}
                    <div className="flex items-center justify-center md:justify-end gap-2">
                        <span className="text-sm text-muted-foreground">Showing</span>
                        <span className="px-3 py-1 rounded-full bg-primary/20 text-primary font-mono text-sm font-semibold">
                            {filteredPlayers.length} / {allPlayers.length}
                        </span>
                    </div>
                </motion.div>

                {/* Main Content Grid */}
                <div className="grid grid-cols-1 xl:grid-cols-5 gap-6">
                    {/* Left Column: Stats & Controls */}
                    <motion.div 
                         initial={{ opacity: 0, x: -20 }}
                         animate={{ opacity: 1, x: 0 }}
                         transition={{ delay: 0.2, duration: 0.5 }}
                         className="xl:col-span-1 space-y-6"
                    >
                        <Overview players={allPlayers} />
                        <QuickActions players={filteredPlayers} sendCommand={sendCommand} />
                        <ConfigPanel config={data?.config} />
                    </motion.div>

                    {/* Right Column: Visualization & Data */}
                    <motion.div 
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        transition={{ delay: 0.3, duration: 0.5 }}
                        className="xl:col-span-4 space-y-6"
                    >
                        <RadiationChart players={filteredPlayers} />
                        
                        <div className="glass rounded-xl overflow-hidden">
                            <div className="p-6">
                                <div className="flex items-center justify-between mb-6">
                                    <h3 className="font-semibold text-lg tracking-tight flex items-center gap-3">
                                        <div className="h-2.5 w-2.5 rounded-full bg-primary pulse-ring" />
                                        Active Players
                                    </h3>
                                    <span className="text-sm text-muted-foreground font-mono">
                                        Real-time tracking
                                    </span>
                                </div>
                                <PlayerTable players={filteredPlayers} />
                            </div>
                        </div>
                    </motion.div>
                </div>
            </motion.main>

            {/* Footer */}
            <footer className="glass-strong border-t border-white/5 mt-8 py-4 px-6 lg:px-10">
                <div className="flex items-center justify-between text-sm text-muted-foreground">
                    <span>Irradiated Mod Debug Interface</span>
                    <span className="font-mono text-xs">v1.0.0 • Minecraft 1.21.1</span>
                </div>
            </footer>
        </div>
    )
}
