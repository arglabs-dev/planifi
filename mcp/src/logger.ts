export type LogLevel = "info" | "error";

export type LogEntry = {
  level: LogLevel;
  message: string;
  action?: string;
  correlationId?: string;
  status?: string;
  latencyMs?: number;
  details?: Record<string, unknown>;
};

export const logger = {
  info(entry: Omit<LogEntry, "level">) {
    console.log(JSON.stringify({ level: "info", ...entry }));
  },
  error(entry: Omit<LogEntry, "level">) {
    console.error(JSON.stringify({ level: "error", ...entry }));
  }
};
