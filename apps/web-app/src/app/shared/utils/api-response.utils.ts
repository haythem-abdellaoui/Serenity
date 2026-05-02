import { Observable, map } from 'rxjs';
import { ApiResponseDTO } from '../../models/api-response.model';

export function unwrapApiResponse<T>(source: Observable<ApiResponseDTO<T>>): Observable<T> {
  return source.pipe(
    map((res) => {
      if (res.data === undefined || res.data === null) {
        throw new Error(res.message || 'Réponse API invalide');
      }
      return res.data;
    })
  );
}

/** Pour DELETE ou réponses sans corps métier utile */
export function unwrapApiVoid(source: Observable<ApiResponseDTO<null | void>>): Observable<void> {
  return source.pipe(map(() => undefined));
}
