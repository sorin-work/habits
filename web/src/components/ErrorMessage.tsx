import { ApiError } from '../api/client';

export function errorText(error: unknown): string {
  if (error instanceof ApiError) return error.message;
  if (error instanceof Error) return error.message;
  return 'Something went wrong';
}

export function ErrorMessage({ error, retry }: { error: unknown; retry?: () => void }) {
  return (
    <div className="error-box" role="alert">
      <span>{errorText(error)}</span>
      {retry && (
        <button type="button" className="link-button" onClick={retry}>
          Try again
        </button>
      )}
    </div>
  );
}
