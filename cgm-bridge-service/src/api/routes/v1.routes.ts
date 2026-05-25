import { Router } from "express";
import { randomUUID } from "node:crypto";
import { GlucoseReading } from "libre-link-unofficial-api";
import { SessionService } from "../../core/session.service";
import { MetricsService } from "../../shared/metrics";
import { requireServiceAuth } from "../middleware/auth.middleware";

const mgDlToMmol = (mgDl: number): number => Math.round((mgDl / 18.0) * 10) / 10;

const trendLabel = (trend: number): string => {
  const map: Record<number, string> = {
    0: "unknown",
    1: "falling_fast",
    2: "falling",
    3: "flat",
    4: "rising",
    5: "rising_fast"
  };
  return map[trend] ?? "unknown";
};

const formatReading = (patientId: string, reading: GlucoseReading) => ({
  patientId,
  mgDl: reading.mgDl,
  mmol: mgDlToMmol(reading.mgDl),
  trend: trendLabel(Number(reading.trend)),
  timestamp: reading.timestamp.toISOString()
});

const problem = (traceId: string, code: string, title: string, status: number, detail: string) => ({
  type: "about:blank",
  title,
  status,
  code,
  detail,
  traceId
});

const parseCreateSessionBody = (payload: unknown): { email: string; password: string } | null => {
  if (!payload || typeof payload !== "object") return null;
  const body = payload as Record<string, unknown>;
  const email = typeof body.email === "string" ? body.email.trim() : "";
  const password = typeof body.password === "string" ? body.password : "";
  if (!email || !password) return null;
  return { email, password };
};

export const createV1Router = (sessions: SessionService, metrics: MetricsService, serviceTokens: string[]): Router => {
  const router = Router();
  const secure = requireServiceAuth(serviceTokens);

  router.get("/health/live", (_req, res) => {
    res.status(200).json({ status: "ok" });
  });

  router.get("/health/ready", (_req, res) => {
    res.status(200).json({ status: "ready" });
  });

  router.use(secure);

  router.post("/sessions", async (req, res) => {
    const traceId = req.header("x-request-id") ?? randomUUID();
    const payload = parseCreateSessionBody(req.body);
    if (!payload) {
      return res.status(422).json(problem(traceId, "INVALID_PAYLOAD", "Payload inválido", 422, "Se requieren email y password."));
    }

    try {
      const session = await sessions.createSession(payload);
      metrics.increment("sessions_created_total");
      return res.status(201).json({
        sessionId: session.id,
        email: session.context.userId,
        patients: session.patients,
        expiresAt: new Date(session.expiresAt).toISOString(),
        traceId
      });
    } catch (error) {
      const msg = (error as Error).message;
      const isAuth = /unauthorized|forbidden|invalid|credentials/i.test(msg);
      return res.status(isAuth ? 401 : 503).json(problem(
        traceId,
        isAuth ? "AUTH_FAILED" : "SESSION_CREATION_FAILED",
        isAuth ? "No autorizado" : "No disponible",
        isAuth ? 401 : 503,
        msg
      ));
    }
  });

  router.delete("/sessions/:sessionId", (req, res) => {
    sessions.deleteSession(req.params.sessionId);
    metrics.increment("sessions_deleted_total");
    return res.status(204).send();
  });

  router.get("/sessions/:sessionId/status", (req, res) => {
    const traceId = req.header("x-request-id") ?? randomUUID();
    const status = sessions.getSessionStatus(req.params.sessionId);
    if (!status) return res.status(404).json(problem(traceId, "SESSION_NOT_FOUND", "No encontrado", 404, "Sesión no encontrada o expirada."));
    return res.status(200).json({ sessionId: req.params.sessionId, patients: status, traceId });
  });

  router.get("/sessions/:sessionId/patients", (req, res) => {
    const traceId = req.header("x-request-id") ?? randomUUID();
    const patients = sessions.getPatients(req.params.sessionId);
    if (!patients) return res.status(401).json(problem(traceId, "INVALID_SESSION", "No autorizado", 401, "Sesión inválida o expirada."));

    const data = patients.map(({ reading, ...meta }) => ({
      ...meta,
      reading: reading ? formatReading(meta.patientId, reading) : null
    }));

    metrics.increment("patients_listed_total");
    return res.status(200).json({ count: data.length, patients: data, traceId });
  });

  router.get("/sessions/:sessionId/patients/:patientId/reading", (req, res) => {
    const traceId = req.header("x-request-id") ?? randomUUID();
    const { sessionId, patientId } = req.params;

    const reading = sessions.getReading(sessionId, patientId);
    if (reading === null) {
      const session = sessions.getSession(sessionId);
      if (!session) return res.status(401).json(problem(traceId, "INVALID_SESSION", "No autorizado", 401, "Sesión inválida o expirada."));
      if (!session.context.patientIds.includes(patientId)) return res.status(403).json(problem(traceId, "PATIENT_SCOPE_DENIED", "Prohibido", 403, "La sesión no tiene acceso a este paciente."));
      return res.status(404).json(problem(traceId, "NO_READING", "No encontrado", 404, "Sin lecturas aún para este paciente."));
    }

    metrics.increment("readings_served_total");
    return res.status(200).json({ ...formatReading(patientId, reading), traceId });
  });

  router.get("/sessions/:sessionId/patients/:patientId/history", async (req, res) => {
    const traceId = req.header("x-request-id") ?? randomUUID();
    const { sessionId, patientId } = req.params;

    try {
      const history = await sessions.getHistory(sessionId, patientId);
      metrics.increment("history_served_total");
      return res.status(200).json({
        patientId,
        count: history.length,
        readings: history.map((r) => formatReading(patientId, r)),
        traceId
      });
    } catch (error) {
      const msg = (error as Error).message;
      const isAuth = /inválida|expirada/i.test(msg);
      const isScope = /accesible/i.test(msg);
      const status = isAuth ? 401 : isScope ? 403 : 503;
      const code = isAuth ? "INVALID_SESSION" : isScope ? "PATIENT_SCOPE_DENIED" : "HISTORY_UNAVAILABLE";
      return res.status(status).json(problem(traceId, code, "Error", status, msg));
    }
  });

  router.get("/status", (_req, res) => {
    const status = sessions.getSystemStatus();
    metrics.setGauge("sessions_active", status.sessions.length);
    metrics.setGauge("runtimes_active", status.runtimes.length);
    return res.status(200).json({
      uptimeSeconds: Math.floor(process.uptime()),
      sessions: status.sessions.length,
      runtimes: status.runtimes.length,
      runtimeDetail: status.runtimes,
      metrics: metrics.snapshot()
    });
  });

  return router;
};
