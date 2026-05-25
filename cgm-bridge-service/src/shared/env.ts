import dotenv from "dotenv";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const rootDir = path.resolve(__dirname, "..", "..");

dotenv.config({ path: path.join(rootDir, ".env") });

const toNumber = (value: string | undefined, fallback: number): number => {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
};

export const env = {
  port: toNumber(process.env.PORT, 3001),
  streamIntervalMs: toNumber(process.env.STREAM_INTERVAL_MS, 90_000),
  sessionTtlMs: toNumber(process.env.SESSION_TTL_MS, 12 * 60 * 60 * 1000),
  lluVersion: process.env.LLU_VERSION ?? "4.16.0",
  serviceTokens: (process.env.SERVICE_TOKENS ?? "")
    .split(",")
    .map((token) => token.trim())
    .filter((token) => token.length > 0),
  rawCacheTtlMs: toNumber(process.env.RAW_CACHE_TTL_MS, 30_000),
  historyCacheTtlMs: toNumber(process.env.HISTORY_CACHE_TTL_MS, 180_000),
  patientsCacheTtlMs: toNumber(process.env.PATIENTS_CACHE_TTL_MS, 600_000),
  maxReconnectDelayMs: toNumber(process.env.MAX_RECONNECT_DELAY_MS, 300_000),
  reconnectBaseDelayMs: toNumber(process.env.RECONNECT_BASE_DELAY_MS, 2_000)
};
