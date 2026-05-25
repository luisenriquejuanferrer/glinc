export type LogLevel = "INFO" | "WARN" | "ERROR";

interface LogContext {
  [key: string]: unknown;
}

const write = (level: LogLevel, message: string, context: LogContext = {}): void => {
  const payload = {
    ts: new Date().toISOString(),
    level,
    message,
    ...context
  };
  const serialized = JSON.stringify(payload);
  if (level === "ERROR") {
    console.error(serialized);
    return;
  }
  console.log(serialized);
};

export const logger = {
  info: (message: string, context?: LogContext) => write("INFO", message, context),
  warn: (message: string, context?: LogContext) => write("WARN", message, context),
  error: (message: string, context?: LogContext) => write("ERROR", message, context)
};
