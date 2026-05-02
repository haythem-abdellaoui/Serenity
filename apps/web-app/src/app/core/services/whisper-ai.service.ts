import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface WhisperTranscribeResponse {
  text: string;
  language?: string;
  duration?: number;
}

export interface WhisperTranslateResponse {
  translated_text: string;
  provider: string;
  note?: string | null;
}

@Injectable({ providedIn: 'root' })
export class WhisperAiService {
  private readonly base = `${environment.apiUrl}/whisper`;

  constructor(private readonly http: HttpClient) {}

  transcribeChunk(blob: Blob, filename: string, sourceLang = 'auto'): Observable<WhisperTranscribeResponse> {
    const body = new FormData();
    body.append('file', blob, filename);
    body.append('sourceLang', sourceLang);
    return this.http.post<WhisperTranscribeResponse>(`${this.base}/transcribe`, body);
  }

  translate(text: string, targetLang: string, sourceLang = 'auto'): Observable<WhisperTranslateResponse> {
    return this.http
      .post<WhisperTranslateResponse & { translatedText?: string }>(`${this.base}/translate`, {
        text,
        targetLang,
        sourceLang
      })
      .pipe(
        map((r) => ({
          translated_text: (r.translated_text ?? r.translatedText ?? '').trim(),
          provider: r.provider ?? 'unknown',
          note: r.note
        }))
      );
  }

  health(): Observable<{ status: string; model_present: boolean; lara_configured?: boolean }> {
    return this.http.get<{ status: string; model_present: boolean; lara_configured?: boolean }>(`${this.base}/health`);
  }
}
