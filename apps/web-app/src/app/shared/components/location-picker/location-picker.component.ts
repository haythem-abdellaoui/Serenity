import { AfterViewInit, Component, ElementRef, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges, ViewChild } from '@angular/core';
import * as L from 'leaflet';

export interface PickerLocation {
  latitude: number;
  longitude: number;
}

export interface PickerMarker {
  latitude: number;
  longitude: number;
  label?: string;
  primary?: boolean;
}

@Component({
  selector: 'app-location-picker',
  templateUrl: './location-picker.component.html',
  styleUrls: ['./location-picker.component.scss']
})
export class LocationPickerComponent implements AfterViewInit, OnChanges, OnDestroy {
  @ViewChild('mapElement', { static: true }) mapElement!: ElementRef<HTMLDivElement>;

  @Input() latitude: number | null = null;
  @Input() longitude: number | null = null;
  @Input() readonly = false;
  @Input() markers: PickerMarker[] = [];

  @Output() locationSelected = new EventEmitter<PickerLocation>();

  selectedLocation: PickerLocation | null = null;

  private map: L.Map | null = null;
  private marker: L.Marker | null = null;
  private markerLayer: L.LayerGroup | null = null;
  private readonly defaultCenter: L.LatLngTuple = [30.0444, 31.2357];
  private readonly defaultZoom = 12;
  private readonly selectedZoom = 16;

  ngAfterViewInit(): void {
    this.initializeMap();
    this.syncMapView();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) {
      return;
    }

    if (changes['markers'] || changes['latitude'] || changes['longitude'] || changes['readonly']) {
      this.syncMapView();
    }
  }

  ngOnDestroy(): void {
    this.map?.remove();
    this.map = null;
    this.marker = null;
  }

  private initializeMap(): void {
    const markerIcon = L.icon({
      iconUrl: 'assets/leaflet/marker-icon.png',
      shadowUrl: 'assets/leaflet/marker-shadow.png',
      iconRetinaUrl: 'assets/leaflet/marker-icon-2x.png',
      iconSize: [25, 41],
      iconAnchor: [12, 41],
      popupAnchor: [1, -34],
      shadowSize: [41, 41]
    });

    this.map = L.map(this.mapElement.nativeElement, {
      center: this.defaultCenter,
      zoom: this.defaultZoom,
      zoomControl: true
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap contributors'
    }).addTo(this.map);

    this.markerLayer = L.layerGroup().addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      if (this.readonly || this.hasDisplayMarkers()) {
        return;
      }
      this.setMarker(event.latlng.lat, event.latlng.lng, true);
    });

    this.marker = L.marker(this.defaultCenter, { draggable: !this.readonly, icon: markerIcon });
    this.marker.on('dragend', () => {
      if (this.readonly || this.hasDisplayMarkers()) {
        return;
      }
      const latLng = this.marker?.getLatLng();
      if (!latLng) {
        return;
      }
      this.setMarker(latLng.lat, latLng.lng, true, false);
    });
  }

  private syncMapView(): void {
    if (!this.map) {
      return;
    }

    if (this.marker) {
      if (this.readonly || this.hasDisplayMarkers()) {
        this.marker.dragging?.disable();
      } else {
        this.marker.dragging?.enable();
      }
    }

    if (this.hasDisplayMarkers()) {
      this.renderDisplayMarkers();
      return;
    }

    this.clearDisplayMarkers();
    this.syncFromInputs();
  }

  private syncFromInputs(): void {
    if (this.latitude == null || this.longitude == null) {
      return;
    }

    this.setMarker(this.latitude, this.longitude, false);
  }

  private renderDisplayMarkers(): void {
    if (!this.map || !this.markerLayer) {
      return;
    }

    this.markerLayer.clearLayers();

    if (this.marker && this.map.hasLayer(this.marker)) {
      this.map.removeLayer(this.marker);
    }

    const validMarkers = this.markers.filter((marker) => this.isValidCoordinate(marker.latitude, marker.longitude));
    if (validMarkers.length === 0) {
      return;
    }

    const bounds = L.latLngBounds([]);
    validMarkers.forEach((item) => {
      const mapMarker = L.marker([item.latitude, item.longitude], {
        icon: this.resolveDisplayMarkerIcon(!!item.primary)
      });

      if (item.label) {
        mapMarker.bindTooltip(item.label, { direction: 'top', offset: [0, -12] });
      }

      mapMarker.addTo(this.markerLayer as L.LayerGroup);
      bounds.extend([item.latitude, item.longitude]);
    });

    if (validMarkers.length === 1) {
      this.map.setView([validMarkers[0].latitude, validMarkers[0].longitude], 14);
      return;
    }

    this.map.fitBounds(bounds.pad(0.22), { maxZoom: 13 });
  }

  private clearDisplayMarkers(): void {
    this.markerLayer?.clearLayers();

    const shouldShowSelectionMarker = this.selectedLocation != null
      || this.isValidCoordinate(this.latitude ?? NaN, this.longitude ?? NaN);

    if (shouldShowSelectionMarker && this.marker && this.map && !this.map.hasLayer(this.marker)) {
      this.marker.addTo(this.map);
    }
  }

  private setMarker(lat: number, lng: number, emit: boolean, moveMap = true): void {
    if (!this.map || !this.marker) {
      return;
    }

    const normalized = this.normalizeCoordinates(lat, lng);

    const rounded = {
      latitude: this.round(normalized.latitude),
      longitude: this.round(normalized.longitude)
    };

    this.selectedLocation = rounded;
    this.marker.setLatLng([rounded.latitude, rounded.longitude]);

    if (!this.map.hasLayer(this.marker)) {
      this.marker.addTo(this.map);
    }

    if (moveMap) {
      this.map.setView([rounded.latitude, rounded.longitude], this.selectedZoom);
    }

    if (emit) {
      this.locationSelected.emit(rounded);
    }
  }

  private round(value: number): number {
    return Number(value.toFixed(6));
  }

  private normalizeCoordinates(lat: number, lng: number): PickerLocation {
    const normalizedLatitude = Math.max(-90, Math.min(90, lat));
    const normalizedLongitude = ((((lng + 180) % 360) + 360) % 360) - 180;

    return {
      latitude: normalizedLatitude,
      longitude: normalizedLongitude
    };
  }

  private hasDisplayMarkers(): boolean {
    return Array.isArray(this.markers) && this.markers.length > 0;
  }

  private isValidCoordinate(latitude: number, longitude: number): boolean {
    return Number.isFinite(latitude)
      && Number.isFinite(longitude)
      && latitude >= -90
      && latitude <= 90
      && longitude >= -180
      && longitude <= 180;
  }

  private resolveDisplayMarkerIcon(primary: boolean): L.DivIcon {
    const className = primary ? 'pharmacy-pin pharmacy-pin-primary' : 'pharmacy-pin';
    return L.divIcon({
      className,
      html: '<span></span>',
      iconSize: [20, 20],
      iconAnchor: [10, 10]
    });
  }
}
