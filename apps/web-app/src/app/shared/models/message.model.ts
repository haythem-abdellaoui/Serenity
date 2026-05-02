export interface MessageDTO {
  id: number;
  conversationId: number;
  senderId: number;
  content: string;
  createdAt: string; // ou Date si tu préfères
  updatedAt?: string; // optionnel
}
