import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface MotivationalQuote {
  text: string;
  author: string;
}

interface ZenQuoteApiItem {
  q: string;
  a: string;
}

interface AllOriginsResponse {
  contents: string;
  status: {
    http_code: number;
  };
}

interface QuotableResponse {
  content: string;
  author: string;
}

@Injectable({
  providedIn: 'root'
})
export class ZenQuotesService {

  private readonly ZEN_QUOTES_URL = 'https://zenquotes.io/api/random';
  private readonly JINA_PROXY_BASE_URL = 'https://r.jina.ai/http://zenquotes.io/api/random';
  private readonly QUOTABLE_URL = 'https://api.quotable.io/random';

  private readonly FALLBACK_QUOTES: MotivationalQuote[] = [
    {
      text: 'Take a deep breath. You are not alone. Every day is a new beginning.',
      author: 'Serenity'
    },
    {
      text: 'You have survived difficult days before, and you will survive this one too.',
      author: 'Serenity'
    },
    {
      text: 'Small steps still move you forward. Be gentle with yourself today.',
      author: 'Serenity'
    },
    {
      text: 'Healing is not linear. Progress can be quiet and still be real.',
      author: 'Serenity'
    }
  ];
  private lastFallbackQuoteIndex = -1;

  constructor(private readonly http: HttpClient) {}

  getRandomQuote(): Observable<MotivationalQuote> {
    // Cache-bust proxied calls so users are less likely to receive stale repeated quotes.
    const targetUrl = `${this.ZEN_QUOTES_URL}?_=${Date.now()}`;
    const allOriginsGetUrl = `https://api.allorigins.win/get?url=${encodeURIComponent(targetUrl)}`;
    const allOriginsRawUrl = `https://api.allorigins.win/raw?url=${encodeURIComponent(targetUrl)}`;
    const jinaProxyUrl = `${this.JINA_PROXY_BASE_URL}?_=${Date.now()}`;

    return this.http.get<ZenQuoteApiItem[]>(targetUrl).pipe(
      map(items => this.toQuoteOrThrow(items)),
      catchError(() => this.http.get(allOriginsRawUrl, { responseType: 'text' }).pipe(
        map(raw => this.parseRawOrThrow(raw))
      )),
      catchError(() => this.http.get<AllOriginsResponse>(allOriginsGetUrl).pipe(
        map(response => this.parseRawOrThrow(response?.contents))
      )),
      catchError(() => this.http.get(jinaProxyUrl, { responseType: 'text' }).pipe(
        map(raw => this.parseRawOrThrow(raw))
      )),
      catchError(() => this.http.get<QuotableResponse>(`${this.QUOTABLE_URL}?_=${Date.now()}`).pipe(
        map(item => this.toQuotableQuoteOrThrow(item))
      )),
      catchError(() => of(this.getRandomFallbackQuote()))
    );
  }

  getFallbackQuote(): MotivationalQuote {
    return this.getRandomFallbackQuote();
  }

  private toQuoteOrThrow(items: ZenQuoteApiItem[] | null | undefined): MotivationalQuote {
    const first = items?.[0];
    if (!first?.q) {
      throw new Error('Invalid quote payload');
    }

    return {
      text: first.q,
      author: first.a || 'Unknown'
    };
  }

  private parseRawOrThrow(raw: string | null | undefined): MotivationalQuote {
    if (!raw) {
      throw new Error('Empty quote payload');
    }

    try {
      const parsed = JSON.parse(raw) as ZenQuoteApiItem[];
      return this.toQuoteOrThrow(parsed);
    } catch {
      const extracted = this.extractJsonArray(raw);
      if (!extracted) {
        throw new Error('Failed to parse quote payload');
      }

      const parsed = JSON.parse(extracted) as ZenQuoteApiItem[];
      return this.toQuoteOrThrow(parsed);
    }
  }

  private toQuotableQuoteOrThrow(item: QuotableResponse | null | undefined): MotivationalQuote {
    const text = item?.content?.trim();
    if (!text) {
      throw new Error('Invalid quotable payload');
    }

    return {
      text,
      author: item?.author?.trim() || 'Unknown'
    };
  }

  private extractJsonArray(raw: string): string | null {
    const start = raw.indexOf('[');
    const end = raw.lastIndexOf(']');
    if (start === -1 || end === -1 || end <= start) {
      return null;
    }

    return raw.slice(start, end + 1);
  }

  private getRandomFallbackQuote(): MotivationalQuote {
    if (this.FALLBACK_QUOTES.length === 1) {
      return this.FALLBACK_QUOTES[0];
    }

    let randomIndex = Math.floor(Math.random() * this.FALLBACK_QUOTES.length);
    while (randomIndex === this.lastFallbackQuoteIndex) {
      randomIndex = Math.floor(Math.random() * this.FALLBACK_QUOTES.length);
    }

    this.lastFallbackQuoteIndex = randomIndex;
    return this.FALLBACK_QUOTES[randomIndex];
  }
}

