export const openApiSpec = {
  openapi: "3.0.3",
  info: {
    title: "CGM Bridge Service",
    version: "1.0.0",
    description: "Puente HTTP entre LibreLink Up (Abbott) y el backend Spring Boot. Lee glucosa en tiempo real de múltiples pacientes en paralelo."
  },
  servers: [{ url: "/v1", description: "API v1" }],
  components: {
    securitySchemes: {
      bearerAuth: {
        type: "http",
        scheme: "bearer",
        description: "SERVICE_TOKEN definido en .env. Opcional si SERVICE_TOKENS está vacío."
      }
    },
    schemas: {
      Reading: {
        type: "object",
        properties: {
          patientId: { type: "string", example: "abc-123" },
          mgDl: { type: "number", example: 120 },
          mmol: { type: "number", example: 6.7 },
          trend: {
            type: "string",
            enum: ["rising_fast", "rising", "flat", "falling", "falling_fast", "unknown"],
            example: "flat"
          },
          timestamp: { type: "string", format: "date-time", example: "2026-05-13T12:34:56Z" }
        }
      },
      PatientMeta: {
        type: "object",
        properties: {
          patientId: { type: "string", example: "abc-123" },
          firstName: { type: "string", example: "Juan" },
          lastName: { type: "string", example: "García" }
        }
      },
      PatientWithReading: {
        allOf: [
          { $ref: "#/components/schemas/PatientMeta" },
          {
            type: "object",
            properties: {
              reading: {
                nullable: true,
                allOf: [{ $ref: "#/components/schemas/Reading" }]
              }
            }
          }
        ]
      },
      Problem: {
        type: "object",
        properties: {
          type: { type: "string", example: "about:blank" },
          title: { type: "string", example: "No autorizado" },
          status: { type: "number", example: 401 },
          code: { type: "string", example: "INVALID_SESSION" },
          detail: { type: "string", example: "Sesión inválida o expirada." },
          traceId: { type: "string", example: "uuid-v4" }
        }
      }
    }
  },
  security: [{ bearerAuth: [] }],
  paths: {
    "/health/live": {
      get: {
        tags: ["Health"],
        summary: "Liveness probe",
        security: [],
        responses: { "200": { description: "Servicio vivo", content: { "application/json": { schema: { type: "object", properties: { status: { type: "string", example: "ok" } } } } } } }
      }
    },
    "/health/ready": {
      get: {
        tags: ["Health"],
        summary: "Readiness probe",
        security: [],
        responses: { "200": { description: "Servicio listo", content: { "application/json": { schema: { type: "object", properties: { status: { type: "string", example: "ready" } } } } } } }
      }
    },
    "/sessions": {
      post: {
        tags: ["Sesiones"],
        summary: "Crear sesión",
        description: "Hace login en LibreLink Up, descubre todos los pacientes de la cuenta y arranca un stream por cada uno.",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["email", "password"],
                properties: {
                  email: { type: "string", format: "email", example: "usuario@email.com" },
                  password: { type: "string", format: "password", example: "mipassword" }
                }
              }
            }
          }
        },
        responses: {
          "201": {
            description: "Sesión creada",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    sessionId: { type: "string", example: "uuid-v4" },
                    email: { type: "string", example: "usuario@email.com" },
                    patients: { type: "array", items: { $ref: "#/components/schemas/PatientMeta" } },
                    expiresAt: { type: "string", format: "date-time" },
                    traceId: { type: "string" }
                  }
                }
              }
            }
          },
          "401": { description: "Credenciales LibreLink inválidas", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } },
          "422": { description: "Payload inválido", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } },
          "503": { description: "LibreLink Up no disponible", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } }
        }
      }
    },
    "/sessions/{sessionId}": {
      delete: {
        tags: ["Sesiones"],
        summary: "Cerrar sesión",
        parameters: [{ name: "sessionId", in: "path", required: true, schema: { type: "string" } }],
        responses: { "204": { description: "Sesión eliminada" } }
      }
    },
    "/sessions/{sessionId}/status": {
      get: {
        tags: ["Sesiones"],
        summary: "Estado del stream por paciente",
        parameters: [{ name: "sessionId", in: "path", required: true, schema: { type: "string" } }],
        responses: {
          "200": {
            description: "Estado de streams",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    sessionId: { type: "string" },
                    patients: {
                      type: "array",
                      items: {
                        type: "object",
                        properties: {
                          patientId: { type: "string" },
                          stream: {
                            type: "object",
                            properties: {
                              state: { type: "string", enum: ["idle", "connecting", "streaming", "backoff", "degraded"] },
                              loggedIn: { type: "boolean" },
                              streaming: { type: "boolean" },
                              consecutiveFailures: { type: "number" },
                              lastError: { type: "string", nullable: true },
                              lastSuccessAt: { type: "string", format: "date-time", nullable: true }
                            }
                          }
                        }
                      }
                    },
                    traceId: { type: "string" }
                  }
                }
              }
            }
          },
          "404": { description: "Sesión no encontrada", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } }
        }
      }
    },
    "/sessions/{sessionId}/patients": {
      get: {
        tags: ["Pacientes"],
        summary: "Dashboard — todos los pacientes con lectura actual",
        parameters: [{ name: "sessionId", in: "path", required: true, schema: { type: "string" } }],
        responses: {
          "200": {
            description: "Lista de pacientes con última lectura",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    count: { type: "number" },
                    patients: { type: "array", items: { $ref: "#/components/schemas/PatientWithReading" } },
                    traceId: { type: "string" }
                  }
                }
              }
            }
          },
          "401": { description: "Sesión inválida", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } }
        }
      }
    },
    "/sessions/{sessionId}/patients/{patientId}/reading": {
      get: {
        tags: ["Lecturas"],
        summary: "Lectura actual de un paciente",
        parameters: [
          { name: "sessionId", in: "path", required: true, schema: { type: "string" } },
          { name: "patientId", in: "path", required: true, schema: { type: "string" } }
        ],
        responses: {
          "200": { description: "Lectura actual", content: { "application/json": { schema: { $ref: "#/components/schemas/Reading" } } } },
          "401": { description: "Sesión inválida", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } },
          "403": { description: "Paciente no accesible desde esta sesión", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } },
          "404": { description: "Sin lecturas aún", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } }
        }
      }
    },
    "/sessions/{sessionId}/patients/{patientId}/history": {
      get: {
        tags: ["Lecturas"],
        summary: "Histórico de lecturas de un paciente",
        parameters: [
          { name: "sessionId", in: "path", required: true, schema: { type: "string" } },
          { name: "patientId", in: "path", required: true, schema: { type: "string" } }
        ],
        responses: {
          "200": {
            description: "Histórico",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    patientId: { type: "string" },
                    count: { type: "number" },
                    readings: { type: "array", items: { $ref: "#/components/schemas/Reading" } },
                    traceId: { type: "string" }
                  }
                }
              }
            }
          },
          "401": { description: "Sesión inválida", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } },
          "403": { description: "Paciente no accesible", content: { "application/json": { schema: { $ref: "#/components/schemas/Problem" } } } }
        }
      }
    },
    "/status": {
      get: {
        tags: ["Sistema"],
        summary: "Estado general del sistema",
        responses: {
          "200": {
            description: "Métricas y estado de runtimes",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    uptimeSeconds: { type: "number" },
                    sessions: { type: "number" },
                    runtimes: { type: "number" },
                    runtimeDetail: { type: "array", items: { type: "object" } },
                    metrics: { type: "object" }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
};
