export interface FieldError {
  field: string | null;
  message: string;
}

/** Standard envelope returned by every FitTrack API endpoint. */
export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  errors?: FieldError[];
  timestamp: string;
  path?: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
