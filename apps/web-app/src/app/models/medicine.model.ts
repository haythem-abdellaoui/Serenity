export interface Medicine {
  id: number;
  name: string;
  description: string | null;
  sideEffects: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface MedicineRequest {
  name: string;
  description?: string;
  sideEffects?: string;
}

export interface OpenFDAMedicine {
  name: string;
  description: string;
  sideEffects: string;
}
