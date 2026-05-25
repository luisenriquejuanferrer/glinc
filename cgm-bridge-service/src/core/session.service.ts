import { GlucoseReading } from "libre-link-unofficial-api";
import { PatientRuntimeService, RuntimeCredentials } from "./patient-runtime.service";
import { StreamStatus } from "./librelink.service";
import { InMemorySessionStore, SessionEnvelope, PatientMeta } from "./session.store";

export interface CreateSessionPayload {
  email: string;
  password: string;
}

export class SessionService {
  private readonly ttlMs: number;
  private readonly store = new InMemorySessionStore();
  private readonly runtime: PatientRuntimeService;
  private readonly cleanupTimer: NodeJS.Timeout;

  constructor(options: {
    ttlMs: number;
    lluVersion: string;
    streamIntervalMs: number;
    reconnectBaseDelayMs: number;
    maxReconnectDelayMs: number;
    rawCacheTtlMs: number;
    historyCacheTtlMs: number;
    patientsCacheTtlMs: number;
  }) {
    this.ttlMs = options.ttlMs;
    this.runtime = new PatientRuntimeService({
      lluVersion: options.lluVersion,
      streamIntervalMs: options.streamIntervalMs,
      reconnectBaseDelayMs: options.reconnectBaseDelayMs,
      maxReconnectDelayMs: options.maxReconnectDelayMs,
      rawCacheTtlMs: options.rawCacheTtlMs,
      historyCacheTtlMs: options.historyCacheTtlMs,
      patientsCacheTtlMs: options.patientsCacheTtlMs
    });
    const cleanupEveryMs = Math.max(60_000, Math.floor(this.ttlMs / 4));
    this.cleanupTimer = setInterval(() => this.cleanupExpired(), cleanupEveryMs);
    this.cleanupTimer.unref();
  }

  async createSession(payload: CreateSessionPayload): Promise<SessionEnvelope> {
    const userId = payload.email;
    const discovered = await this.runtime.discoverPatients(payload.email, payload.password);
    const patientIds = discovered.map((p) => p.patientId);

    const credentials: Omit<RuntimeCredentials, "patientId"> = {
      email: payload.email,
      password: payload.password
    };

    for (const patient of discovered) {
      this.runtime.getOrCreate(
        { userId, patientId: patient.patientId },
        { ...credentials, patientId: patient.patientId }
      );
    }

    await Promise.all(
      patientIds.map((patientId) =>
        this.runtime.readLatest({ userId, patientId }).catch(() => undefined)
      )
    );

    const patients: PatientMeta[] = discovered.map((p) => ({
      patientId: p.patientId,
      firstName: p.firstName,
      lastName: p.lastName
    }));

    return this.store.create({ userId, patientIds }, patients, this.ttlMs);
  }

  getSession(sessionId: string): SessionEnvelope | null {
    const session = this.store.get(sessionId);
    if (!session) return null;
    if (session.expiresAt <= Date.now()) {
      this.deleteSession(sessionId);
      return null;
    }
    this.store.touch(sessionId, this.ttlMs);
    return session;
  }

  deleteSession(sessionId: string): void {
    const removed = this.store.delete(sessionId);
    if (!removed) return;
    this.runtime.releaseAll({
      userId: removed.context.userId,
      patientIds: removed.context.patientIds
    });
  }

  getPatients(sessionId: string): Array<PatientMeta & { reading: GlucoseReading | null }> | null {
    const session = this.getSession(sessionId);
    if (!session) return null;
    return session.patients.map((patient) => {
      const runtime = this.runtime.getRuntime({
        userId: session.context.userId,
        patientId: patient.patientId
      });
      return { ...patient, reading: runtime?.service.getLatest() ?? null };
    });
  }

  getReading(sessionId: string, patientId: string): GlucoseReading | null {
    const session = this.getSession(sessionId);
    if (!session) return null;
    if (!session.context.patientIds.includes(patientId)) return null;
    const runtime = this.runtime.getRuntime({ userId: session.context.userId, patientId });
    return runtime?.service.getLatest() ?? null;
  }

  async getHistory(sessionId: string, patientId: string): Promise<GlucoseReading[]> {
    const session = this.getSession(sessionId);
    if (!session) throw new Error("Sesión inválida o expirada");
    if (!session.context.patientIds.includes(patientId)) throw new Error("Paciente no accesible desde esta sesión");
    const runtime = this.runtime.getRuntime({ userId: session.context.userId, patientId });
    if (!runtime) throw new Error("Runtime no disponible");
    return runtime.service.history();
  }

  getSessionStatus(sessionId: string): Array<{ patientId: string; stream: StreamStatus }> | null {
    const session = this.getSession(sessionId);
    if (!session) return null;
    return session.context.patientIds.map((patientId) => {
      const runtime = this.runtime.getRuntime({ userId: session.context.userId, patientId });
      return {
        patientId,
        stream: runtime?.service.status ?? {
          loggedIn: false,
          streaming: false,
          consecutiveFailures: 0,
          state: "idle" as const
        }
      };
    });
  }

  getSystemStatus() {
    const sessions = this.store.list();
    const runtimes = this.runtime.listStatus();
    return { sessions, runtimes };
  }

  private cleanupExpired(): void {
    const expired = this.store.deleteExpired(Date.now());
    for (const session of expired) {
      this.runtime.releaseAll({
        userId: session.context.userId,
        patientIds: session.context.patientIds
      });
    }
  }

  async shutdown(): Promise<void> {
    clearInterval(this.cleanupTimer);
    for (const session of this.store.list()) {
      this.runtime.releaseAll({
        userId: session.context.userId,
        patientIds: session.context.patientIds
      });
    }
    await this.runtime.shutdown();
  }
}
