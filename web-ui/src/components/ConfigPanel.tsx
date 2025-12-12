
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Settings, Check, X, Hash } from "lucide-react";
import { ServerConfig } from "@/hooks/useRadiationData";
import { motion } from "framer-motion";

interface ConfigPanelProps {
    config?: ServerConfig;
}

interface ConfigItemProps {
    label: string;
    value: string | number | boolean;
    type?: 'number' | 'boolean';
    delay?: number;
}

function ConfigItem({ label, value, type = 'number', delay = 0 }: ConfigItemProps) {
    return (
        <motion.div 
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay, duration: 0.3 }}
            className="flex justify-between items-center p-3 rounded-lg bg-secondary/20 border border-white/5 hover:border-white/10 transition-all"
        >
            <span className="text-sm text-muted-foreground">{label}</span>
            {type === 'boolean' ? (
                <div className={`flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${
                    value ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'
                }`}>
                    {value ? <Check className="h-3 w-3" /> : <X className="h-3 w-3" />}
                    {value ? 'Enabled' : 'Disabled'}
                </div>
            ) : (
                <span className="font-mono text-primary font-semibold flex items-center gap-1">
                    <Hash className="h-3 w-3 text-muted-foreground" />
                    {value}
                </span>
            )}
        </motion.div>
    );
}

export function ConfigPanel({ config }: ConfigPanelProps) {
    if (!config) return null;
    
    return (
        <Card className="glass border-white/5 overflow-hidden">
            <CardHeader className="pb-3 border-b border-white/5">
                <CardTitle className="flex items-center gap-3 text-lg">
                    <div className="p-2 rounded-lg bg-purple-500/10">
                        <Settings className="h-5 w-5 text-purple-400" />
                    </div>
                    Configuration
                </CardTitle>
            </CardHeader>
            <CardContent className="grid gap-2 pt-4">
                <ConfigItem 
                    label="Block Radiation Range" 
                    value={config.blockRadiationRange}
                    delay={0}
                />
                <ConfigItem 
                    label="Shielding System" 
                    value={config.shieldingEnabled}
                    type="boolean"
                    delay={0.05}
                />
                <ConfigItem 
                    label="Default Shielding" 
                    value={config.defaultShielding}
                    delay={0.1}
                />
            </CardContent>
        </Card>
    );
}

