import { LibreLinkClient, GlucoseReading } from "libre-link-unofficial-api";

export interface LibreLinkServiceOptions {
  email: string;
  password: string;
  patientId?: string;
  lluVersion?: string;
  cache?: boolean;
}

export interface StreamStatus {
  loggedIn: boolean;
  streaming: boolean;
  lastError?: string;
  lastSuccessAt?: string;
  consecutiveFailures: number;
  nextRetryAt?: string;
  state: "idle" | "connecting" | "streaming" | "backoff" | "degraded";
}

interface CacheEntry<T> {
  value: T;
  expiresAt: number;
}

interface StreamStrategy {
  intervalMs: number;
  baseDelayMs: number;
  maxDelayMs: number;
  rawCacheTtlMs: number;
  historyCacheTtlMs: number;
  patientsCacheTtlMs: number;
}

export class LibreLinkService {
  private client: LibreLinkClient;
  private latestReading: GlucoseReading | null = null;
  private rawReading: CacheEntry<unknown> | null = null;
  private historyCache: CacheEntry<GlucoseReading[]> | null = null;
  private patientsCache: CacheEntry<unknown> | null = null;
  private inFlightRaw: Promise<unknown> | null = null;
  private inFlightHistory: Promise<GlucoseReading[]> | null = null;
  private inFlightPatients: Promise<unknown> | null = null;
  private streaming = false;
  private shutdown = false;
  private lastError?: string;
  private lastSuccessAt?: Date;
  private consecutiveFailures = 0;
  private nextRetryAt?: Date;
  private state: StreamStatus["state"] = "idle";
  private strategy: StreamStrategy = {
    intervalMs: 90_000,
    baseDelayMs: 2_000,
    maxDelayMs: 300_000,
    rawCacheTtlMs: 30_000,
    historyCacheTtlMs: 180_000,
    patientsCacheTtlMs: 600_000
  };

  constructor(options: LibreLinkServiceOptions) {
    this.client = new LibreLinkClient({
      email: options.email,
      password: options.password,
      patientId: options.patientId,
      lluVersion: options.lluVersion,
      cache: options.cache ?? true
    });
  }

  get status(): StreamStatus {
    return {
      loggedIn: Boolean(this.client.me),
      streaming: this.streaming,
      lastError: this.lastError,
      lastSuccessAt: this.lastSuccessAt?.toISOString(),
      consecutiveFailures: this.consecutiveFailures,
      nextRetryAt: this.nextRetryAt?.toISOString(),
      state: this.state
    };
  }

  getLatest(): GlucoseReading | null {
    return this.latestReading;
  }

  getRawLatest(): unknown | null {
    return this.rawReading?.value ?? null;
  }

  getHistoryCache(): GlucoseReading[] {
    return this.historyCache?.value ?? [];
  }

  async login(): Promise<void> {
    try {
      await this.client.login();
      this.lastError = undefined;
    } catch (error) {
      this.lastError = (error as Error).message;
      throw error;
    }
  }

  async readLatest(): Promise<GlucoseReading> {
    const reading = await this.client.read();
    this.latestReading = reading;
    this.lastSuccessAt = new Date();
    return reading;
  }

  async fetchRaw(): Promise<unknown> {
    const now = Date.now();
    if (this.rawReading && this.rawReading.expiresAt > now) {
      return this.rawReading.value;
    }
    if (this.inFlightRaw) return this.inFlightRaw;

    this.inFlightRaw = this.client.fetchReading().then((raw) => {
      this.rawReading = { value: raw, expiresAt: Date.now() + this.strategy.rawCacheTtlMs };
      return raw;
    }).finally(() => {
      this.inFlightRaw = null;
    });
    return this.inFlightRaw;
  }

  async history(): Promise<GlucoseReading[]> {
    const now = Date.now();
    if (this.historyCache && this.historyCache.expiresAt > now) {
      return this.historyCache.value;
    }
    if (this.inFlightHistory) return this.inFlightHistory;

    this.inFlightHistory = this.client.history().then((history) => {
      this.historyCache = { value: history, expiresAt: Date.now() + this.strategy.historyCacheTtlMs };
      return history;
    }).finally(() => {
      this.inFlightHistory = null;
    });
    return this.inFlightHistory;
  }

  async connections(): Promise<unknown> {
    const now = Date.now();
    if (this.patientsCache && this.patientsCache.expiresAt > now) {
      return this.patientsCache.value;
    }
    if (this.inFlightPatients) return this.inFlightPatients;

    this.inFlightPatients = this.client.fetchConnections().then((connections) => {
      this.patientsCache = { value: connections, expiresAt: Date.now() + this.strategy.patientsCacheTtlMs };
      return connections;
    }).finally(() => {
      this.inFlightPatients = null;
    });
    return this.inFlightPatients;
  }

  startStream(options: Partial<StreamStrategy>): void {
    if (this.streaming) return;
    this.strategy = { ...this.strategy, ...options };
    this.shutdown = false;
    this.streaming = true;
    this.state = "connecting";
    void this.runWithRetry();
  }

  stopStream(): void {
    this.streaming = false;
    this.shutdown = true;
    this.state = "idle";
  }

  private async runWithRetry(): Promise<void> {
    while (this.streaming && !this.shutdown) {
      try {
        this.state = "connecting";
        await this.login();
        this.state = "streaming";
        this.consecutiveFailures = 0;
        await this.runStream();
      } catch (error) {
        this.lastError = (error as Error).message;
        this.consecutiveFailures += 1;
        if (!this.streaming || this.shutdown) break;

        const power = Math.min(this.consecutiveFailures, 8);
        const maxDelay = Math.min(this.strategy.maxDelayMs, this.strategy.baseDelayMs * 2 ** power);
        const retryInMs = Math.floor(Math.random() * maxDelay);
        this.nextRetryAt = new Date(Date.now() + retryInMs);
        this.state = this.consecutiveFailures >= 8 ? "degraded" : "backoff";
        await new Promise<void>((resolve) => setTimeout(resolve, retryInMs));
      }
    }
    this.streaming = false;
    if (!this.shutdown) this.state = "idle";
  }

  private async runStream(): Promise<void> {
    try {
      for await (const reading of this.client.stream(this.strategy.intervalMs)) {
        if (!this.streaming) break;
        this.latestReading = reading;
        this.lastSuccessAt = new Date();
        this.lastError = undefined;
      }
    } catch (error) {
      this.lastError = (error as Error).message;
      throw error;
    }
  }
}
