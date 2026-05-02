/** Paramètres de pagination alignés sur les contrôleurs Spring (page, size, sortBy, direction) */
export interface PageQuery {
  page?: number;
  size?: number;
  sortBy?: string;
  direction?: 'asc' | 'desc';
}
