
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Users, Activity, AlertTriangle, Flame, TrendingUp } from "lucide-react";
import { PlayerData } from "@/hooks/useRadiationData";
import { motion } from "framer-motion";

interface OverviewProps {
    players: PlayerData[];
}

interface StatCardProps {
    icon: React.ReactNode;
    label: string;
    value: number | string;
    color: 'blue' | 'green' | 'red' | 'orange' | 'purple';
    delay?: number;
}

function StatCard({ icon, label, value, color, delay = 0 }: StatCardProps) {
    const colorClasses = {
        blue: 'bg-blue-500/10 text-blue-500 border-blue-500/20',
        green: 'bg-green-500/10 text-green-500 border-green-500/20',
        red: 'bg-red-500/10 text-red-500 border-red-500/20',
        orange: 'bg-orange-500/10 text-orange-500 border-orange-500/20',
        purple: 'bg-purple-500/10 text-purple-500 border-purple-500/20',
    };

    const iconColorClasses = {
        blue: 'bg-blue-500/20 text-blue-400',
        green: 'bg-green-500/20 text-green-400',
        red: 'bg-red-500/20 text-red-400',
        orange: 'bg-orange-500/20 text-orange-400',
        purple: 'bg-purple-500/20 text-purple-400',
    };

    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay, duration: 0.3 }}
            className={`stat-card flex items-center justify-between p-4 rounded-xl border ${colorClasses[color]} backdrop-blur-sm`}
        >
            <div className="flex items-center gap-3">
                <div className={`p-2.5 rounded-lg ${iconColorClasses[color]}`}>
                    {icon}
                </div>
                <span className="text-sm font-medium text-muted-foreground">{label}</span>
            </div>
            <motion.span 
                key={value}
                initial={{ scale: 1.2, opacity: 0 }}
                animate={{ scale: 1, opacity: 1 }}
                className="text-3xl font-bold font-mono"
            >
                {value}
            </motion.span>
        </motion.div>
    );
}

export function Overview({ players }: OverviewProps) {
    const avgRad = players.length > 0 
        ? Math.round(players.reduce((acc, p) => acc + p.radiationLevel, 0) / players.length)
        : 0;

    const highestRad = players.length > 0
        ? Math.max(...players.map(p => p.radiationLevel))
        : 0;
    
    // Count unique nearby hot blocks
    const hotBlocks = new Set<string>();
    players.forEach(p => {
        if (Array.isArray(p.nearbyRadioactiveBlocks)) {
            p.nearbyRadioactiveBlocks.forEach((b: any) => {
                if (b.block) hotBlocks.add(b.block);
            });
        }
    });

    return (
        <Card className="glass border-white/5 overflow-hidden">
            <CardHeader className="pb-3 border-b border-white/5">
                <CardTitle className="flex items-center gap-3 text-lg">
                    <div className="p-2 rounded-lg bg-blue-500/10">
                        <TrendingUp className="h-5 w-5 text-blue-400" />
                    </div>
                    System Overview
                </CardTitle>
            </CardHeader>
            <CardContent className="space-y-3 pt-4">
                <StatCard
                    icon={<Users className="h-5 w-5" />}
                    label="Players Online"
                    value={players.length}
                    color="blue"
                    delay={0}
                />
                <StatCard
                    icon={<Activity className="h-5 w-5" />}
                    label="Average Radiation"
                    value={avgRad}
                    color="green"
                    delay={0.05}
                />
                <StatCard
                    icon={<AlertTriangle className="h-5 w-5" />}
                    label="Peak Exposure"
                    value={highestRad}
                    color="red"
                    delay={0.1}
                />
                <StatCard
                    icon={<Flame className="h-5 w-5" />}
                    label="Active Sources"
                    value={hotBlocks.size}
                    color="orange"
                    delay={0.15}
                />
            </CardContent>
        </Card>
    );
}

