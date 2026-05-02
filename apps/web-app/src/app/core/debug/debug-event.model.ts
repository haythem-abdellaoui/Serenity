export type DebugCategory =
  | 'NAVIGATION'
  | 'HTTP'
  | 'UI_ACTION'
  | 'STATE'
  | 'ERROR'
  | 'AUTH'
  | 'MISSING_ROUTE';

export type DebugSeverity = 'info' | 'warn' | 'error';

export interface DebugEvent {
  id: number;
  timestamp: string;
  category: DebugCategory;
  severity: DebugSeverity;
  action: string;
  route: string;
  details?: Record<string, unknown>;
}
