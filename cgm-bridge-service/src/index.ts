import express from "express";
import { Request, Response, NextFunction } from "express";
import { Server } from "node:http";
import swaggerUi from "swagger-ui-express";
import { env } from "./shared/env";
import { openApiSpec } from "./api/openapi";
import { SessionService } from "./core/session.service";
import { createV1Router } from "./api/routes/v1.routes";
import { MetricsService } from "./shared/metrics";
import { logger } from "./shared/logger";

const bootstrap = async (): Promise<void> => {
  const app = express();
  app.use(express.json());
  app.use("/docs", swaggerUi.serve, swaggerUi.setup(openApiSpec));

  const metrics = new MetricsService();
  const sessionService = new SessionService({
    ttlMs: env.sessionTtlMs,
    lluVersion: env.lluVersion,
    streamIntervalMs: env.streamIntervalMs,
    reconnectBaseDelayMs: env.reconnectBaseDelayMs,
    maxReconnectDelayMs: env.maxReconnectDelayMs,
    rawCacheTtlMs: env.rawCacheTtlMs,
    historyCacheTtlMs: env.historyCacheTtlMs,
    patientsCacheTtlMs: env.patientsCacheTtlMs
  });

  app.use("/v1", createV1Router(sessionService, metrics, env.serviceTokens));
  logger.info("bridge_started", { mode: "multi-tenant-v1" });

  app.use((_req, res) => {
    return res.status(404).json({ message: "Ruta no encontrada" });
  });

  app.use((error: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (error instanceof SyntaxError && "body" in (error as object)) {
      return res.status(400).json({ message: "JSON inválido en el body" });
    }
    return res.status(500).json({ message: "Error interno del servidor" });
  });

  const server: Server = app.listen(env.port, () => {
    logger.info("http_listening", { port: env.port });
  });

  const shutdown = async (signal: string): Promise<void> => {
    logger.warn("shutdown_signal_received", { signal });
    await new Promise<void>((resolve) => server.close(() => resolve()));
    await sessionService.shutdown();
    logger.info("shutdown_complete");
    process.exit(0);
  };

  process.on("SIGINT", () => { void shutdown("SIGINT"); });
  process.on("SIGTERM", () => { void shutdown("SIGTERM"); });
};

bootstrap().catch((error) => {
  logger.error("bootstrap_failed", { error: (error as Error).message });
  process.exit(1);
});
