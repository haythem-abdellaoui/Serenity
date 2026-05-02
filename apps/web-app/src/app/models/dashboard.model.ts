export interface DashboardStats {
  totalPatients: number;
  activeRecords: number;
  activePrescriptions: number;
  severityLow: number;
  severityMedium: number;
  severityHigh: number;

  // AI Recommendation Statistics
  aiRecommendedCount: number;
  totalPrescriptionItems: number;
  aiAcceptanceRate: number;
  topAiMedicines: { name: string; count: number }[];
}
