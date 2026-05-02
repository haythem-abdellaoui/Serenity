import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, map, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../../core/services/auth.service';
import { MonitoringService } from '../../../../core/services/monitoring.service';
import { EmotionalTriggerResponse, MoodEntryResponse } from '../../../../shared/models/mood.model';

interface PatientDot {
  patientId: number;
  patientName: string;
  avatarUrl?: string;
  entries: MoodEntryResponse[];
  latestMood: number;
  latestDate: Date;
  averageMood: number;
  moodTrend: number;
  triggerLoad: number;
  crisisRate: number;
  riskScore: number;
}

interface ChartPoint {
  x: number;
  y: number;
  /** Averaged mood when several entries share the same time bucket. */
  mood: number;
  at: Date;
  /** Representative entry id (first in bucket). */
  entryId: number;
  /** How many raw entries were averaged into this point (same calendar second). */
  aggregatedCount: number;
}

interface PatientSeries {
  patientId: number;
  patientName: string;
  color: string;
  path: string;
  points: ChartPoint[];
  xTicks: { x: number; label: string }[];
}

@Component({
  selector: 'app-mood-dashboard',
  templateUrl: './mood-dashboard.component.html',
  styleUrls: ['./mood-dashboard.component.scss']
})
export class MoodDashboardComponent implements OnInit {
  loading = true;
  errorMessage = '';

  chartWidth = 960;
  chartHeight = 300;
  padding = 48;

  /** Y-axis top of scale (baseline is 0). Mood entries remain 1–10. */
  readonly axisMoodMax = 10;

  doctorId: number | null = null;
  patientDots: PatientDot[] = [];
  patientSeries: PatientSeries[] = [];
  selectedPatient: PatientDot | null = null;
  hoveredTooltip: {
    patientId: number;
    patientName: string;
    mood: number;
    at: Date;
    riskScore: number;
    moodTrend: number;
    aggregatedCount: number;
  } | null = null;

  private triggerByMoodEntryId: Record<number, EmotionalTriggerResponse[]> = {};
  private readonly moodMin = 1;
  private readonly moodMax = 10;
  private readonly seriesPalette = [
    '#0d9488',
    '#2563eb',
    '#d97706',
    '#7c3aed',
    '#db2777',
    '#059669',
    '#ea580c',
    '#0891b2'
  ];

  constructor(
    private readonly monitoringService: MonitoringService,
    private readonly authService: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const currentUser = this.authService.getCurrentUser();
    if (!this.authService.isDoctor() || !currentUser?.userId) {
      this.router.navigate(['/monitoring']);
      return;
    }

    this.doctorId = currentUser.userId;
    this.loadDashboard(currentUser.userId);
  }

  get xAxisY(): number {
    return this.chartHeight - this.padding;
  }

  get yAxisX(): number {
    return this.padding;
  }

  get yTicks(): number[] {
    return [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
  }

  get portfolioAverageMood(): number {
    if (!this.patientDots.length) {
      return 0;
    }
    return this.patientDots.reduce((acc, p) => acc + p.averageMood, 0) / this.patientDots.length;
  }

  get highRiskCount(): number {
    return this.patientDots.filter((p) => p.riskScore >= 70).length;
  }

  get crisisPatientCount(): number {
    return this.patientDots.filter((p) => p.crisisRate > 0).length;
  }

  get selectedPatientName(): string {
    return this.selectedPatient?.patientName || 'No patient selected';
  }

  get rankedPatients(): PatientDot[] {
    return [...this.patientDots].sort((a, b) => b.riskScore - a.riskScore);
  }

  goBack(): void {
    this.router.navigate(['/monitoring']);
  }

  selectPatient(patient: PatientDot): void {
    this.selectedPatient = patient;
  }

  onChartPointKeydown(event: KeyboardEvent, patient: PatientDot): void {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      this.selectPatient(patient);
    }
  }

  onChartPointEnter(
    patient: PatientDot,
    mood: number,
    at: Date,
    aggregatedCount = 1
  ): void {
    this.hoveredTooltip = {
      patientId: patient.patientId,
      patientName: patient.patientName,
      mood,
      at,
      riskScore: patient.riskScore,
      moodTrend: patient.moodTrend,
      aggregatedCount
    };
  }

  onChartPointLeave(): void {
    this.hoveredTooltip = null;
  }

  getTrendLabel(value: number): string {
    if (value > 0.15) return `+${value.toFixed(2)} improving`;
    if (value < -0.15) return `${value.toFixed(2)} declining`;
    return 'stable';
  }

  moodLevel(rawMoodScore: number): number {
    return this.normalizeMood(rawMoodScore);
  }

  getRiskBand(risk: number): string {
    if (risk >= 70) return 'High risk';
    if (risk >= 40) return 'Watch';
    return 'Stable';
  }

  getRiskClass(risk: number): string {
    if (risk >= 70) return 'risk-high';
    if (risk >= 40) return 'risk-medium';
    return 'risk-low';
  }

  getEntryTriggers(entryId: number): EmotionalTriggerResponse[] {
    return this.triggerByMoodEntryId[entryId] || [];
  }

  private loadDashboard(doctorId: number): void {
    this.loading = true;
    this.errorMessage = '';

    this.monitoringService.getMoodEntriesForDoctor(doctorId).pipe(
      switchMap((entries) => {
        if (!entries.length) {
          return of({ entries, triggersByEntry: {} as Record<number, EmotionalTriggerResponse[]> });
        }

        const requests = entries.map((entry) =>
          this.monitoringService.getTriggersByMoodEntryId(entry.id).pipe(
            map((triggers) => ({ moodEntryId: entry.id, triggers })),
            catchError(() => of({ moodEntryId: entry.id, triggers: [] as EmotionalTriggerResponse[] }))
          )
        );

        return forkJoin(requests).pipe(
          map((triggerResults) => {
            const triggerMap: Record<number, EmotionalTriggerResponse[]> = {};
            triggerResults.forEach((item) => {
              triggerMap[item.moodEntryId] = item.triggers;
            });
            return { entries, triggersByEntry: triggerMap };
          })
        );
      })
    ).subscribe({
      next: ({ entries, triggersByEntry }) => {
        this.triggerByMoodEntryId = triggersByEntry;
        this.patientDots = this.buildPatientDots(entries, triggersByEntry);
        this.patientSeries = this.buildPatientSeries(this.patientDots);

        const requestedPatientIdRaw = this.route.snapshot.queryParamMap.get('patientId');
        const requestedPatientId = requestedPatientIdRaw ? Number(requestedPatientIdRaw) : NaN;
        this.selectedPatient = this.patientDots.find((p) => p.patientId === requestedPatientId) || this.rankedPatients[0] || null;

        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || err?.message || 'Failed to load doctor dashboard.';
      }
    });
  }

  private buildPatientDots(
    entries: MoodEntryResponse[],
    triggerMap: Record<number, EmotionalTriggerResponse[]>
  ): PatientDot[] {
    if (!entries.length) {
      return [];
    }

    const grouped = new Map<number, MoodEntryResponse[]>();

    entries.forEach((entry) => {
      if (!grouped.has(entry.patientId)) {
        grouped.set(entry.patientId, []);
      }
      grouped.get(entry.patientId)!.push(entry);
    });

    const dots = Array.from(grouped.entries()).map(([patientId, patientEntries]) => {
      const sorted = [...patientEntries].sort((a, b) => +new Date(a.createdAt) - +new Date(b.createdAt));
      const moods = sorted.map((e) => this.normalizeMood(e.moodScore));
      const averageMood = this.average(moods);
      const moodTrend = moods.length > 1 ? moods[moods.length - 1] - moods[0] : 0;
      const crisisRate = moods.length > 0 ? moods.filter((m) => m <= 3).length / moods.length : 0;
      const triggerValues = sorted.flatMap((e) => (triggerMap[e.id] || []).map((t) => t.intensity));
      const triggerLoad = triggerValues.length ? this.average(triggerValues) : 0;

      const latest = sorted[sorted.length - 1];
      const latestMood = this.normalizeMood(latest.moodScore);
      const latestDate = new Date(latest.createdAt);

      const lowMoodFactor = (this.moodMax - latestMood) / (this.moodMax - this.moodMin);
      const triggerFactor = triggerLoad / 10;
      const negativeTrendFactor = moodTrend < 0 ? Math.min(Math.abs(moodTrend) / 4, 1) : 0;
      const riskScore = this.clamp(
        100 * (0.35 * lowMoodFactor + 0.30 * crisisRate + 0.20 * triggerFactor + 0.15 * negativeTrendFactor),
        0,
        100
      );

      return {
        patientId,
        patientName: patientEntries[patientEntries.length - 1].patientName || `Patient #${patientId}`,
        avatarUrl: patientEntries[patientEntries.length - 1].patientAvatarUrl || undefined,
        crisisRate,
        latestMood,
        latestDate,
        averageMood,
        moodTrend,
        triggerLoad,
        riskScore,
        entries: sorted
      } as PatientDot;
    });

    return dots.sort((a, b) => +a.latestDate - +b.latestDate);
  }

  private buildPatientSeries(dots: PatientDot[]): PatientSeries[] {
    if (!dots.length) {
      return [];
    }

    const plotW = this.chartWidth - this.padding * 2;
    const plotH = this.chartHeight - this.padding * 2;

    const toY = (moodScore: number) => {
      const n = this.normalizeMood(moodScore);
      const normalized = n / this.axisMoodMax;
      return this.chartHeight - this.padding - normalized * plotH;
    };

    return dots.map((d, i) => {
      const color = this.seriesPalette[i % this.seriesPalette.length];
      const aggregated = this.aggregateMoodsByLocalCalendarDay(d.entries);
      if (aggregated.length === 0) {
        return {
          patientId: d.patientId,
          patientName: d.patientName,
          color,
          path: '',
          points: [],
          xTicks: []
        };
      }

      let minPlot = aggregated[0].plotMs;
      let maxPlot = aggregated[aggregated.length - 1].plotMs;
      if (minPlot === maxPlot) {
        minPlot -= 60 * 60 * 1000;
        maxPlot += 60 * 60 * 1000;
      }
      const plotRange = Math.max(1, maxPlot - minPlot);
      const toX = (plotMs: number) => this.padding + ((plotMs - minPlot) / plotRange) * plotW;

      const xTicks = this.buildXTimeTicks(minPlot, plotRange, plotW);

      const rawPoints: ChartPoint[] = aggregated.map((row) => ({
        x: toX(row.plotMs),
        y: toY(row.avgMood),
        mood: this.normalizeMood(row.avgMood),
        at: row.at,
        entryId: row.representativeEntryId,
        aggregatedCount: row.aggregatedCount
      }));
      const points = this.mergePointsWithDuplicatePlotX(rawPoints, toY);

      const xy = points.map((p) => ({ x: p.x, y: p.y }));
      let path = '';
      if (points.length === 1) {
        path = `M ${points[0].x} ${points[0].y}`;
      } else {
        path = this.catmullRomBezierPath(xy, 10);
      }

      return {
        patientId: d.patientId,
        patientName: d.patientName,
        color,
        path,
        points,
        xTicks
      };
    });
  }

  /**
   * Start of local calendar day (00:00) in ms for the given instant.
   */
  private startOfLocalDayMs(ms: number): number {
    const d = new Date(ms);
    return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
  }

  /**
   * Exactly one (x, y) per calendar day: all mood entries on the same local date are averaged.
   * X uses local noon on that day so each day maps to a single abscissa (strict f: time → mood).
   */
  private aggregateMoodsByLocalCalendarDay(
    entries: MoodEntryResponse[]
  ): {
    plotMs: number;
    avgMood: number;
    at: Date;
    representativeEntryId: number;
    aggregatedCount: number;
  }[] {
    const sorted = [...entries].sort((a, b) => +new Date(a.createdAt) - +new Date(b.createdAt));
    type Bucket = {
      moodSum: number;
      count: number;
      representativeEntryId: number;
    };
    const buckets = new Map<number, Bucket>();

    for (const e of sorted) {
      const ms = +new Date(e.createdAt);
      const dayStart = this.startOfLocalDayMs(ms);
      const mood = this.normalizeMood(e.moodScore);
      const existing = buckets.get(dayStart);
      if (!existing) {
        buckets.set(dayStart, {
          moodSum: mood,
          count: 1,
          representativeEntryId: e.id
        });
      } else {
        existing.moodSum += mood;
        existing.count += 1;
      }
    }

    const noonMs = 12 * 60 * 60 * 1000;
    const keys = Array.from(buckets.keys()).sort((a, b) => a - b);
    return keys.map((dayStart) => {
      const b = buckets.get(dayStart)!;
      const avgMood = b.moodSum / b.count;
      const plotMs = dayStart + noonMs;
      return {
        plotMs,
        avgMood,
        at: new Date(plotMs),
        representativeEntryId: b.representativeEntryId,
        aggregatedCount: b.count
      };
    });
  }

  /**
   * Safety net: if two buckets still map to the same pixel x, merge into one weighted average.
   */
  private mergePointsWithDuplicatePlotX(
    points: ChartPoint[],
    toY: (mood: number) => number
  ): ChartPoint[] {
    if (points.length <= 1) {
      return points;
    }
    const merged: ChartPoint[] = [];
    for (const p of points) {
      const last = merged[merged.length - 1];
      if (last && Math.abs(p.x - last.x) < 0.75) {
        const n = last.aggregatedCount + p.aggregatedCount;
        const wMood =
          (last.mood * last.aggregatedCount + p.mood * p.aggregatedCount) / n;
        last.mood = this.normalizeMood(wMood);
        last.y = toY(wMood);
        last.aggregatedCount = n;
      } else {
        merged.push({ ...p });
      }
    }
    return merged;
  }

  /**
   * Catmull–Rom spline as cubic Bézier segments (C¹ continuous, sinus-like smooth turns).
   * Larger `tension` → gentler bends and less overshoot past data points.
   */
  private catmullRomBezierPath(pts: { x: number; y: number }[], tension = 8): string {
    if (pts.length === 0) {
      return '';
    }
    if (pts.length === 1) {
      return `M ${pts[0].x} ${pts[0].y}`;
    }
    const n = pts.length;
    const get = (idx: number) => pts[Math.max(0, Math.min(n - 1, idx))];
    let d = `M ${pts[0].x} ${pts[0].y}`;
    for (let i = 0; i < n - 1; i++) {
      const p0 = get(i - 1);
      const p1 = get(i);
      const p2 = get(i + 1);
      const p3 = get(i + 2);
      const cp1x = p1.x + (p2.x - p0.x) / tension;
      const cp1y = p1.y + (p2.y - p0.y) / tension;
      const cp2x = p2.x - (p3.x - p1.x) / tension;
      const cp2y = p2.y - (p3.y - p1.y) / tension;
      d += ` C ${cp1x} ${cp1y}, ${cp2x} ${cp2y}, ${p2.x} ${p2.y}`;
    }
    return d;
  }

  private buildXTimeTicks(
    minMs: number,
    rangeMs: number,
    plotW: number
  ): { x: number; label: string }[] {
    const segments = 4;
    const ticks: { x: number; label: string }[] = [];
    for (let i = 0; i <= segments; i++) {
      const t = minMs + (rangeMs * i) / segments;
      const x = this.padding + (plotW * i) / segments;
      const d = new Date(t);
      const label = d.toLocaleString(undefined, {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
      ticks.push({ x, label });
    }
    return ticks;
  }

  private average(values: number[]): number {
    if (!values.length) {
      return 0;
    }
    return values.reduce((acc, v) => acc + v, 0) / values.length;
  }

  private normalizeMood(rawMoodScore: number): number {
    return this.clamp(rawMoodScore, this.moodMin, this.moodMax);
  }

  patientDotById(patientId: number): PatientDot | undefined {
    return this.patientDots.find((p) => p.patientId === patientId);
  }

  private getTriggerPenalty(triggers: EmotionalTriggerResponse[]): number {
    if (!triggers.length) {
      return 0;
    }
    const avgIntensity = this.average(triggers.map((t) => t.intensity));
    // Max penalty is 0.4 mood points when trigger load is severe.
    return this.clamp(avgIntensity / 25, 0, 0.4);
  }


  private clamp(value: number, min: number, max: number): number {
    return Math.max(min, Math.min(max, value));
  }
}

