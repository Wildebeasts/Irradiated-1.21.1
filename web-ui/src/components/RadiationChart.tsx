
import { useState, useEffect } from 'react';
import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid, Legend, Bar, BarChart } from "recharts";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { PlayerData } from "@/hooks/useRadiationData";
import { Activity, BarChart3, TrendingUp } from "lucide-react";

interface RadiationChartProps {
    players: PlayerData[];
}

interface ChartDataPoint {
    time: string;
    [key: string]: string | number;
}

export function RadiationChart({ players }: RadiationChartProps) {
    const [history, setHistory] = useState<ChartDataPoint[]>([]);

    useEffect(() => {
        const now = new Date();
        const timeString = now.toLocaleTimeString([], { hour12: true, hour: '2-digit', minute: '2-digit', second: '2-digit' });
        
        const newDataPoint: ChartDataPoint = {
            time: timeString,
        };

        let hasActivePlayers = false;

        players.forEach(player => {
            newDataPoint[player.name] = player.radiationLevel;
            hasActivePlayers = true;
        });

        if (hasActivePlayers) {
            setHistory(prev => {
                const newHistory = [...prev, newDataPoint];
                if (newHistory.length > 30) { // Keep last 30 updates
                    return newHistory.slice(1);
                }
                return newHistory;
            });
        }
    }, [players]);

    const playerNames = players.map(p => p.name);
    const colors = ["#22c55e", "#eab308", "#ef4444", "#3b82f6", "#a855f7", "#06b6d4", "#f97316", "#ec4899"];

    // Prepare data for bar chart
    const barData = players.map(p => ({
        name: p.name,
        radiation: p.radiationLevel,
        fill: p.radiationLevel > 60 ? '#ef4444' : p.radiationLevel > 30 ? '#eab308' : '#22c55e'
    }));

    return (
        <Card className="glass border-white/5 overflow-hidden">
            <CardHeader className="border-b border-white/5">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                        <div className="p-2 rounded-lg bg-green-500/10">
                            <Activity className="h-5 w-5 text-green-400" />
                        </div>
                        <div>
                            <CardTitle className="text-lg">Radiation Analytics</CardTitle>
                            <CardDescription className="text-muted-foreground">Real-time radiation exposure monitoring</CardDescription>
                        </div>
                    </div>
                    <div className="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full bg-primary/10 border border-primary/20">
                        <div className="h-2 w-2 rounded-full bg-primary pulse-ring" />
                        <span className="text-xs font-mono text-primary">Live Feed</span>
                    </div>
                </div>
            </CardHeader>
            <CardContent className="pt-6">
                <Tabs defaultValue="overtime" className="space-y-4">
                    <TabsList className="bg-secondary/30 p-1">
                        <TabsTrigger value="overtime" className="data-[state=active]:bg-primary/20 data-[state=active]:text-primary gap-2">
                            <TrendingUp className="h-4 w-4" />
                            Timeline
                        </TabsTrigger>
                        <TabsTrigger value="current" className="data-[state=active]:bg-primary/20 data-[state=active]:text-primary gap-2">
                            <BarChart3 className="h-4 w-4" />
                            Current
                        </TabsTrigger>
                    </TabsList>
                    <TabsContent value="overtime" className="h-[350px]">
                        {history.length === 0 ? (
                            <div className="flex h-full w-full items-center justify-center text-muted-foreground">
                                <div className="text-center space-y-2">
                                    <Activity className="h-10 w-10 mx-auto opacity-50" />
                                    <p>Waiting for data...</p>
                                </div>
                            </div>
                        ) : (
                            <ResponsiveContainer width="100%" height="100%">
                                <LineChart data={history}>
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" />
                                    <XAxis 
                                        dataKey="time" 
                                        stroke="hsl(var(--muted-foreground))" 
                                        fontSize={11} 
                                        tickLine={false} 
                                        axisLine={false}
                                        tick={{ fill: 'hsl(var(--muted-foreground))' }}
                                    />
                                    <YAxis 
                                        stroke="hsl(var(--muted-foreground))" 
                                        fontSize={11} 
                                        tickLine={false} 
                                        axisLine={false}
                                        tickFormatter={(value) => `${value}`}
                                        tick={{ fill: 'hsl(var(--muted-foreground))' }}
                                        domain={[0, 100]}
                                    />
                                    <Tooltip
                                        contentStyle={{ 
                                            backgroundColor: "rgba(17, 24, 39, 0.95)", 
                                            border: "1px solid rgba(255,255,255,0.1)",
                                            borderRadius: "12px",
                                            boxShadow: "0 8px 32px rgba(0,0,0,0.4)"
                                        }}
                                        itemStyle={{ color: "#f3f4f6" }}
                                        labelStyle={{ color: "#9ca3af", marginBottom: "8px" }}
                                    />
                                    <Legend 
                                        wrapperStyle={{ paddingTop: "20px" }}
                                    />
                                    {playerNames.map((name, index) => (
                                        <Line 
                                            key={name} 
                                            type="monotone" 
                                            dataKey={name} 
                                            stroke={colors[index % colors.length]} 
                                            strokeWidth={2.5}
                                            dot={false}
                                            activeDot={{ r: 5, strokeWidth: 2, stroke: 'rgba(0,0,0,0.3)' }}
                                            isAnimationActive={false}
                                        />
                                    ))}
                                </LineChart>
                            </ResponsiveContainer>
                        )}
                    </TabsContent>
                    <TabsContent value="current" className="h-[350px]">
                         {players.length === 0 ? (
                            <div className="flex h-full w-full items-center justify-center text-muted-foreground">
                                <div className="text-center space-y-2">
                                    <BarChart3 className="h-10 w-10 mx-auto opacity-50" />
                                    <p>No active players</p>
                                </div>
                            </div>
                         ) : (
                            <ResponsiveContainer width="100%" height="100%">
                                <BarChart data={barData} layout="vertical">
                                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" horizontal={false} />
                                    <XAxis 
                                        type="number" 
                                        domain={[0, 100]} 
                                        stroke="hsl(var(--muted-foreground))"
                                        fontSize={11}
                                        tickLine={false}
                                        axisLine={false}
                                        tick={{ fill: 'hsl(var(--muted-foreground))' }}
                                    />
                                    <YAxis 
                                        type="category" 
                                        dataKey="name" 
                                        stroke="hsl(var(--muted-foreground))"
                                        fontSize={12}
                                        tickLine={false}
                                        axisLine={false}
                                        tick={{ fill: 'hsl(var(--muted-foreground))' }}
                                        width={80}
                                    />
                                    <Tooltip
                                        contentStyle={{ 
                                            backgroundColor: "rgba(17, 24, 39, 0.95)", 
                                            border: "1px solid rgba(255,255,255,0.1)",
                                            borderRadius: "12px",
                                            boxShadow: "0 8px 32px rgba(0,0,0,0.4)"
                                        }}
                                        formatter={(value: number) => [`${value}`, 'Radiation Level']}
                                        labelStyle={{ color: "#9ca3af" }}
                                    />
                                    <Bar 
                                        dataKey="radiation" 
                                        radius={[0, 6, 6, 0]}
                                        maxBarSize={35}
                                    />
                                </BarChart>
                            </ResponsiveContainer>
                         )}
                    </TabsContent>
                </Tabs>
            </CardContent>
        </Card>
    );
}

