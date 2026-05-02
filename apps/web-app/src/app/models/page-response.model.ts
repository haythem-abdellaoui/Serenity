/**
 * Aligné sur tn.esprit.arctic.derbelmicroservice.dto.response.PageResponseDTO
 */
export interface PageResponseDTO<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
