/**
 * Aligné sur tn.esprit.arctic.derbelmicroservice.dto.response.ApiResponseDTO
 */
export interface ApiResponseDTO<T> {
  status: number;
  message: string;
  data: T;
  timestamp?: string;
}
