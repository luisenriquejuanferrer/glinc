import { randomUUID } from "node:crypto";

export interface PatientMeta {
  patientId: string;
  firstName?: string;
  lastName?: string;
}

export interface UserContext {
  userId: string;
  patientIds: string[];
}

export interface SessionEnvelope {
  id: string;
  createdAt: number;
  lastAccessAt: number;
  expiresAt: number;
  context: UserContext;
  patients: PatientMeta[];
}

export class InMemorySessionStore {
  private readonly sessions = new Map<string, SessionEnvelope>();

  create(context: UserContext, patients: PatientMeta[], ttlMs: number): SessionEnvelope {
    const now = Date.now();
    const session: SessionEnvelope = {
      id: randomUUID(),
      createdAt: now,
      lastAccessAt: now,
      expiresAt: now + ttlMs,
      context,
      patients
    };
    this.sessions.set(session.id, session);
    return session;
  }

  get(sessionId: string): SessionEnvelope | null {
    return this.sessions.get(sessionId) ?? null;
  }

  touch(sessionId: string, ttlMs: number): SessionEnvelope | null {
    const session = this.sessions.get(sessionId);
    if (!session) return null;
    const now = Date.now();
    session.lastAccessAt = now;
    session.expiresAt = now + ttlMs;
    return session;
  }

  delete(sessionId: string): SessionEnvelope | null {
    const session = this.sessions.get(sessionId);
    if (!session) return null;
    this.sessions.delete(sessionId);
    return session;
  }

  deleteExpired(now = Date.now()): SessionEnvelope[] {
    const expired: SessionEnvelope[] = [];
    for (const [id, session] of this.sessions.entries()) {
      if (session.expiresAt <= now) {
        this.sessions.delete(id);
        expired.push(session);
      }
    }
    return expired;
  }

  list(): SessionEnvelope[] {
    return Array.from(this.sessions.values());
  }
}
