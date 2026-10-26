import { ErrorCode } from './problem-detail';

export interface UiError {
  readonly code: ErrorCode | null;
  readonly message: string;
  readonly fieldErrors: Readonly<Record<string, readonly string[]>>;
}
