# Irradiated Debug Server Web UI

This directory contains the modern React-based user interface for the Irradiated mod's debug server.

## Tech Stack
- **Framework**: React + Vite
- **Styling**: Tailwind CSS
- **UI Components**: Shadcn UI
- **Icons**: Lucide React
- **Language**: TypeScript

## Development

1. Install dependencies:
   ```bash
   npm install
   ```

2. Start the development server:
   ```bash
   npm run dev
   ```
   
   The app will try to connect to `ws://localhost:8001` by default. You can change this in `src/hooks/useRadiationData.ts` if needed.

## Building for Deployment

To generate the static files for the Minecraft server to serve:

1. Run the build command:
   ```bash
   npm run build
   ```

2. This will create a `dist` directory containing `index.html` and assets.

## Deployment

1. Locate your Minecraft server/client config directory.
2. Create the folder structure: `config/irradiated/web-ui`.
3. Copy the **contents** of the `dist` folder into `config/irradiated/web-ui`.
   - You should have `config/irradiated/web-ui/index.html` and `config/irradiated/web-ui/assets/...`.
4. Start the Minecraft server (or client with debug server enabled).
5. Access the dashboard at `http://localhost:8000` (or your configured port).
