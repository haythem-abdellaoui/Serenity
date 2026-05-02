import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';

export interface Icd10Result {
  code: string;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class Icd10Service {
  private readonly apiUrl = 'https://clinicaltables.nlm.nih.gov/api/icd10cm/v3/search';

  constructor(private readonly http: HttpClient) { }

  search(term: string): Observable<Icd10Result[]> {
    if (!term || term.trim().length < 2) {
      return of([]);
    }

    return this.http
      .get<any>(this.apiUrl, {
        params: {
          sf: 'code,name',
          terms: term.trim(),
          maxList: '7'
        }
      })
      .pipe(
        map((response: any) => {
          // API response format: [totalCount, codeArray, null, [[code, name], ...]]
          const entries: string[][] = response[3] || [];
          return entries.map(([code, name]) => ({ code, name }));
        }),
        catchError(() => of([]))
      );
  }
}
