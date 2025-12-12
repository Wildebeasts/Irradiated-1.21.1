import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { ScrollArea } from "@/components/ui/scroll-area"
import { PlayerData } from "@/hooks/useRadiationData"
import { MapPin, Shield, Droplets, AlertTriangle, Radiation } from "lucide-react"

interface PlayerTableProps {
  players: PlayerData[]
}

function RadiationBar({ level }: { level: number }) {
  const getColor = () => {
    if (level > 60) return 'bg-red-500';
    if (level > 30) return 'bg-yellow-500';
    return 'bg-green-500';
  };

  return (
    <div className="flex items-center gap-3">
      <div className="w-24 h-2 bg-secondary/50 rounded-full overflow-hidden">
        <div 
          className={`h-full ${getColor()} transition-all duration-500`}
          style={{ width: `${Math.min(100, level)}%` }}
        />
      </div>
      <span className={`font-mono font-semibold text-sm ${
        level > 60 ? 'text-red-400' : level > 30 ? 'text-yellow-400' : 'text-green-400'
      }`}>
        {level}
      </span>
    </div>
  );
}

export function PlayerTable({ players }: PlayerTableProps) {
  return (
    <div className="rounded-lg border border-white/5 overflow-hidden">
      <ScrollArea className="h-[500px] custom-scrollbar">
        <Table>
          <TableHeader>
            <TableRow className="border-white/5 hover:bg-transparent">
              <TableHead className="text-muted-foreground font-medium">Player</TableHead>
              <TableHead className="text-muted-foreground font-medium">Location</TableHead>
              <TableHead className="text-muted-foreground font-medium">Biome</TableHead>
              <TableHead className="text-muted-foreground font-medium">Radiation</TableHead>
              <TableHead className="text-muted-foreground font-medium">Exposure</TableHead>
              <TableHead className="text-muted-foreground font-medium">Protection</TableHead>
              <TableHead className="text-muted-foreground font-medium">Status</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {players.length === 0 ? (
                <TableRow className="border-white/5">
                    <TableCell colSpan={7} className="text-center h-32 text-muted-foreground">
                        <div className="flex flex-col items-center gap-2">
                            <Radiation className="h-8 w-8 opacity-50" />
                            <span>No players online</span>
                        </div>
                    </TableCell>
                </TableRow>
            ) : (
                players.map((player) => (
                <TableRow 
                    key={player.uuid}
                    className="border-white/5 table-row-hover"
                >
                    <TableCell className="font-medium">
                        <div className="flex items-center gap-2">
                            <div className="h-8 w-8 rounded-full bg-primary/10 border border-primary/20 flex items-center justify-center text-primary text-sm font-semibold">
                                {player.name.charAt(0).toUpperCase()}
                            </div>
                            <span>{player.name}</span>
                        </div>
                    </TableCell>
                    <TableCell>
                        <div className="flex items-center gap-1.5 text-sm">
                            <MapPin className="h-3.5 w-3.5 text-muted-foreground" />
                            <span className="font-mono text-xs">
                                {player.position.x}, {player.position.y}, {player.position.z}
                            </span>
                        </div>
                        <div className="text-xs text-muted-foreground mt-0.5 pl-5">{player.dimension}</div>
                    </TableCell>
                    <TableCell>
                        <span className="text-sm">{player.biome}</span>
                    </TableCell>
                    <TableCell>
                        <RadiationBar level={player.radiationLevel} />
                    </TableCell>
                    <TableCell>
                        <span className="font-mono text-sm text-muted-foreground">{player.dynamicExposure}</span>
                    </TableCell>
                    <TableCell>
                        <div className="flex items-center gap-1.5">
                            <Shield className="h-3.5 w-3.5 text-blue-400" />
                            <span className="font-mono text-sm text-blue-400">{player.armorProtection}%</span>
                        </div>
                    </TableCell>
                    <TableCell>
                        <div className="flex gap-1.5 flex-wrap">
                            {player.hasRadResistance && (
                                <Badge variant="outline" className="border-blue-500/30 bg-blue-500/10 text-blue-400 text-xs">
                                    <Shield className="h-3 w-3 mr-1" />
                                    Resistant
                                </Badge>
                            )}
                            {player.isInWater && (
                                <Badge variant="outline" className="border-cyan-500/30 bg-cyan-500/10 text-cyan-400 text-xs">
                                    <Droplets className="h-3 w-3 mr-1" />
                                    Water
                                </Badge>
                            )}
                            {player.nearbyRadioactiveBlocks.length > 0 && (
                                <Badge variant="destructive" className="bg-red-500/20 border-red-500/30 text-red-400 text-xs">
                                    <AlertTriangle className="h-3 w-3 mr-1" />
                                    Near Source
                                </Badge>
                            )}
                            {!player.hasRadResistance && !player.isInWater && player.nearbyRadioactiveBlocks.length === 0 && (
                                <span className="text-xs text-muted-foreground">Normal</span>
                            )}
                        </div>
                    </TableCell>
                </TableRow>
                ))
            )}
          </TableBody>
        </Table>
      </ScrollArea>
    </div>
  )
}

