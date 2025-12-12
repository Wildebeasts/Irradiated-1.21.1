
import { useState } from 'react';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { PlayerData } from "@/hooks/useRadiationData";
import { Zap, Eraser, Plus, Target, Users, ChevronRight } from "lucide-react";
import { motion } from "framer-motion";

interface QuickActionsProps {
    players: PlayerData[];
    sendCommand: (cmd: any) => void;
}

export function QuickActions({ players, sendCommand }: QuickActionsProps) {
    const [selectedPlayer, setSelectedPlayer] = useState<string>("all");
    const [customValue, setCustomValue] = useState<string>("0");

    const getTargets = () => {
        if (selectedPlayer === "all") {
            return players.map(p => p.name);
        }
        return [selectedPlayer];
    };

    const handleSet = (val?: number) => {
        const value = val !== undefined ? val : parseInt(customValue);
        getTargets().forEach(name => {
            sendCommand({ action: "set", player: name, level: value });
        });
    };

    const handleAdd = (amount: number) => {
        getTargets().forEach(name => {
            sendCommand({ action: "add", player: name, amount: amount });
        });
    };

    const handleClear = () => {
        getTargets().forEach(name => {
            sendCommand({ action: "clear", player: name });
        });
    };

    return (
        <Card className="glass border-white/5 overflow-hidden">
            <CardHeader className="pb-3 border-b border-white/5">
                <CardTitle className="flex items-center gap-3 text-lg">
                    <div className="p-2 rounded-lg bg-amber-500/10">
                        <Zap className="h-5 w-5 text-amber-400" />
                    </div>
                    Quick Actions
                </CardTitle>
            </CardHeader>
            <CardContent className="space-y-5 pt-4">
                {/* Target Selector */}
                <div className="space-y-2">
                    <Label htmlFor="target" className="text-sm text-muted-foreground flex items-center gap-2">
                        <Target className="h-3.5 w-3.5" />
                        Target Player
                    </Label>
                    <Select value={selectedPlayer} onValueChange={setSelectedPlayer}>
                        <SelectTrigger id="target" className="bg-background/30 border-white/10 focus:border-primary/50">
                            <SelectValue placeholder="Select target" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">
                                <div className="flex items-center gap-2">
                                    <Users className="h-4 w-4" />
                                    All Players ({players.length})
                                </div>
                            </SelectItem>
                            {players.map(p => (
                                <SelectItem key={p.uuid} value={p.name}>{p.name}</SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                    <motion.p 
                        key={selectedPlayer}
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="text-xs text-muted-foreground flex items-center gap-1"
                    >
                        <ChevronRight className="h-3 w-3" />
                        Affecting {selectedPlayer === "all" ? players.length : 1} player{selectedPlayer === "all" && players.length !== 1 ? 's' : ''}
                    </motion.p>
                </div>

                {/* Set Custom Value */}
                <div className="space-y-3">
                    <div className="flex gap-2">
                        <Input 
                            type="number" 
                            value={customValue} 
                            onChange={(e) => setCustomValue(e.target.value)}
                            placeholder="0"
                            className="w-24 bg-background/30 border-white/10 font-mono text-center focus:border-primary/50"
                        />
                        <Button 
                            className="flex-1 bg-primary/20 hover:bg-primary/30 text-primary border border-primary/30 hover:border-primary/50 transition-all" 
                            onClick={() => handleSet()}
                        >
                            <Zap className="mr-2 h-4 w-4" />
                            Set Value
                        </Button>
                    </div>

                    {/* Preset Buttons */}
                    <div className="grid grid-cols-2 gap-2">
                        <Button 
                            variant="secondary" 
                            className="bg-green-500/10 hover:bg-green-500/20 text-green-400 border border-green-500/20 hover:border-green-500/40 transition-all"
                            onClick={() => handleSet(20)}
                        >
                            Low (20)
                        </Button>
                        <Button 
                            variant="secondary" 
                            className="bg-red-500/10 hover:bg-red-500/20 text-red-400 border border-red-500/20 hover:border-red-500/40 transition-all glow-danger"
                            onClick={() => handleSet(80)}
                        >
                            High (80)
                        </Button>
                    </div>

                    {/* Action Buttons */}
                    <div className="grid grid-cols-2 gap-2 pt-2">
                        <Button 
                            variant="outline" 
                            className="bg-blue-500/10 hover:bg-blue-500/20 text-blue-400 border border-blue-500/20 hover:border-blue-500/40 transition-all"
                            onClick={() => handleAdd(10)}
                        >
                            <Plus className="mr-2 h-4 w-4" />
                            +10 All
                        </Button>
                        <Button 
                            variant="destructive" 
                            className="bg-red-500/20 hover:bg-red-500/30 border border-red-500/30 hover:border-red-500/50 transition-all"
                            onClick={handleClear}
                        >
                            <Eraser className="mr-2 h-4 w-4" />
                            Clear
                        </Button>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}

