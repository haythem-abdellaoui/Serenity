import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';

interface NominatimAddressResponse {
  house_number?: string;
  road?: string;
  suburb?: string;
  neighbourhood?: string;
  quarter?: string;
  city?: string;
  town?: string;
  village?: string;
  municipality?: string;
  county?: string;
  state?: string;
  region?: string;
}

interface NominatimReverseResponse {
  display_name?: string;
  address?: NominatimAddressResponse;
}

export interface ReverseGeocodedLocation {
  addressLine: string;
  city: string;
  governorate: string;
}

@Injectable({
  providedIn: 'root'
})
export class LocationGeocodingService {
  private readonly reverseEndpoint = 'https://nominatim.openstreetmap.org/reverse';

  constructor(private readonly http: HttpClient) {}

  reverseGeocode(latitude: number, longitude: number): Observable<ReverseGeocodedLocation | null> {
    const params = new HttpParams()
      .set('format', 'jsonv2')
      .set('addressdetails', '1')
      .set('lat', String(latitude))
      .set('lon', String(longitude))
      .set('accept-language', 'fr,en');

    return this.http.get<NominatimReverseResponse>(this.reverseEndpoint, { params }).pipe(
      map((response) => this.mapReverseResponse(response)),
      catchError(() => of(null))
    );
  }

  private mapReverseResponse(response: NominatimReverseResponse): ReverseGeocodedLocation | null {
    const address = response.address || {};

    const addressLine = this.firstNonEmpty([
      this.joinParts([
        address.house_number,
        address.road
      ]),
      this.joinParts([
        address.road,
        address.suburb
      ]),
      this.joinParts([
        address.neighbourhood,
        address.suburb
      ]),
      response.display_name
    ]);

    const city = this.firstNonEmpty([
      address.city,
      address.town,
      address.village,
      address.municipality,
      address.county
    ]);

    const governorate = this.firstNonEmpty([
      address.state,
      address.region,
      address.county
    ]);

    if (!addressLine && !city && !governorate) {
      return null;
    }

    return {
      addressLine: addressLine || '',
      city: city || '',
      governorate: governorate || ''
    };
  }

  private joinParts(parts: Array<string | undefined>): string {
    return parts
      .map((part) => (part || '').trim())
      .filter((part) => part.length > 0)
      .join(', ');
  }

  private firstNonEmpty(values: Array<string | undefined>): string {
    for (const value of values) {
      const normalized = (value || '').trim();
      if (normalized) {
        return normalized;
      }
    }
    return '';
  }
}
