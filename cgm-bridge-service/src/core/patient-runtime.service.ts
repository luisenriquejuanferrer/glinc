import { GlucoseReading } from "libre-link-unofficial-api";
import { LibreLinkService, StreamStatus } from "./librelink.service";
import { logger } from "../shared/logger";

type RawConnections = { data?: Array<Record<string, unknown>> } | Array<Record<string, unknown>>;

export interface PatientDiscovery {
  patientId: string;
  firstName?: string;
  lastName?: string;
}

export interface RuntimeCredentials {
  email: string;
  password: string;
  patientId: string;
}

interface RuntimeEntry {
  key: string;
  refs: number;
  service: LibreLinkService;
  context: {
    userId: string;
    patientId: string;
  };
}

interface RuntimeOptions {
  lluVersion: string;
  streamIntervalMs: number;
  reconnectBaseDelayMs: number;
  maxReconnectDelayMs: number;
  rawCacheTtlMs: number;
  historyCacheTtlMs: number;
  patientsCacheTtlMs: number;
}

export class PatientRuntimeService {
  private readonly runtimes = new Map<string, RuntimeEntry>();

  constructor(private readonly options: RuntimeOptions) {}

  getOrCreate(context: RuntimeEntry["context"], credentials: RuntimeCredentials): RuntimeEntry {
    const key = this.key(context.userId, context.patientId);
    const existing = this.runtimes.get(key);
    if (existing) {
      existing.refs += 1;
      return existing;
    }

    const service = new LibreLinkService({
      email: credentials.email,
      password: credentials.password,
      patientId: credentials.patientId,
      lluVersion: this.options.lluVersion,
      cache: true
    });

    service.startStream({
      intervalMs: this.options.streamIntervalMs,
      baseDelayMs: this.options.reconnectBaseDelayMs,
      maxDelayMs: this.options.maxReconnectDelayMs,
      rawCacheTtlMs: this.options.rawCacheTtlMs,
      historyCacheTtlMs: this.options.historyCacheTtlMs,
      patientsCacheTtlMs: this.options.patientsCacheTtlMs
    });

    const entry: RuntimeEntry = { key, refs: 1, service, context };
    this.runtimes.set(key, entry);
    logger.info("runtime_created", { userId: context.userId, patientId: context.patientId });
    return entry;
  }

  release(context: { userId: string; patientId: string }): void {
    const key = this.key(context.userId, context.patientId);
    const runtime = this.runtimes.get(key);
    if (!runtime) return;
    runtime.refs -= 1;
    if (runtime.refs <= 0) {
      runtime.service.stopStream();
      this.runtimes.delete(key);
      logger.info("runtime_released", { userId: context.userId, patientId: context.patientId });
    }
  }

  releaseAll(context: { userId: string; patientIds: string[] }): void {
    for (const patientId of context.patientIds) {
      this.release({ userId: context.userId, patientId });
    }
  }

  getRuntime(context: { userId: string; patientId: string }): RuntimeEntry | null {
    return this.runtimes.get(this.key(context.userId, context.patientId)) ?? null;
  }

  listStatus(): Array<{
    userId: string;
    patientId: string;
    refs: number;
    stream: StreamStatus;
    hasLatest: boolean;
  }> {
    return Array.from(this.runtimes.values()).map((runtime) => ({
      userId: runtime.context.userId,
      patientId: runtime.context.patientId,
      refs: runtime.refs,
      stream: runtime.service.status,
      hasLatest: Boolean(runtime.service.getLatest())
    }));
  }

  readLatest(context: { userId: string; patientId: string }): Promise<GlucoseReading> {
    const runtime = this.getRuntime(context);
    if (!runtime) throw new Error("Runtime no encontrado");
    const current = runtime.service.getLatest();
    if (current) return Promise.resolve(current);
    return runtime.service.readLatest();
  }

  async discoverPatients(email: string, password: string): Promise<PatientDiscovery[]> {
    const temp = new LibreLinkService({ email, password, lluVersion: this.options.lluVersion });
    await temp.login();
    const raw = await temp.connections() as RawConnections;
    const items = Array.isArray((raw as { data?: unknown[] })?.data)
      ? (raw as { data: Array<Record<string, unknown>> }).data
      : Array.isArray(raw) ? raw : [];
    if (items.length === 0) throw new Error("No hay pacientes en la cuenta LibreLink.");
    return items
      .map((item) => ({
        patientId: (item.patientId ?? item.id ?? "") as string,
        firstName: item.firstName as string | undefined,
        lastName: item.lastName as string | undefined
      }))
      .filter((p) => Boolean(p.patientId));
  }

  async shutdown(): Promise<void> {
    for (const runtime of this.runtimes.values()) {
      runtime.service.stopStream();
    }
    this.runtimes.clear();
  }

  private key(userId: string, patientId: string): string {
    return `${userId}:${patientId}`;
  }
}
