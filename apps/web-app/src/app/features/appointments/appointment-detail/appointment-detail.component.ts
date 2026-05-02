import { Component, ElementRef, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { AppointmentService } from '../../../core/services/appointment.service';
import { AuthService } from '../../../core/services/auth.service';
import { UserService } from '../../../core/services/user.service';
import { WhisperAiService } from '../../../core/services/whisper-ai.service';
import { UserLookup, formatUserLookupName } from '../../../shared/models/user.model';
import {
  AppointmentResponse,
  CalendarBusySlot,
  TeleconsultationResponse,
  appointmentParticipantDisplayName
} from '../../../shared/models/appointment.model';
import {
  APPOINTMENT_SLOT_DURATION_MINUTES,
  appointmentDateToYmd,
  hasAppointmentTimeOverlap,
  minDateInputYmd,
  validateAppointmentScheduling
} from '../../../shared/utils/appointment-scheduling.utils';

@Component({
  selector: 'app-appointment-detail',
  templateUrl: './appointment-detail.component.html',
  styleUrls: ['./appointment-detail.component.scss']
})
export class AppointmentDetailComponent implements OnInit, OnDestroy {
  private static readonly JITSI_ORIGIN = 'https://meet.jit.si/';
  @ViewChild('translatedArea') private translatedArea?: ElementRef<HTMLTextAreaElement>;

  appointment: AppointmentResponse | null = null;
  tele: TeleconsultationResponse | null = null;
  loading = true;
  errorMessage = '';
  /** Filled via POST /users/lookup/names (browser + gateway); avoids broken server-to-user-service calls. */
  private readonly nameByUserId = new Map<number, UserLookup>();

  /** Reschedule form (same rules as booking). */
  rescheduleDate = '';
  rescheduleTime = '';
  rescheduleBusy: CalendarBusySlot[] = [];
  rescheduleCalMonth = new Date().getMonth() + 1;
  rescheduleCalYear = new Date().getFullYear();
  loadingRescheduleCal = false;
  rescheduleSubmitting = false;
  calendarLoadError = '';
  /** Reschedule form is shown only after clicking “Reschedule”. */
  showReschedulePanel = false;

  /** In-app Jitsi (iframe) instead of redirecting the whole tab. */
  inlineMeetingOpen = false;
  safeMeetingUrl: SafeResourceUrl | null = null;

  /** Live captions: microphone or shared tab / conference audio → gateway → ASR. */
  captionsActive = false;
  captionBusy = false;
  captionError = '';
  liveTranscript = '';
  speechSourceLang: 'auto' | 'en' | 'fr' | 'ar' = 'auto';
  translateTargetLang: 'fr' | 'en' | 'ar' = 'fr';
  translatedText = '';
  translateBusy = false;
  /** Tab capture so mixed Jitsi audio can be transcribed (user must share this tab + tab audio in the browser dialog). */
  captionUseConferenceAudio = true;
  /** Debounced full-transcript translation while captions run. */
  autoTranslateEnabled = true;
  /** Skip STT while local speaker is talking so captions focus on the remote side. */
  suppressOwnSpeechEnabled = true;

  private mediaRecorder: MediaRecorder | null = null;
  private mediaStream: MediaStream | null = null;
  /** Full stream from getDisplayMedia; kept while captioning so tab capture stays active. */
  private conferenceSurfaceStream: MediaStream | null = null;
  /** Stops each finalized WebM/M4A segment so ffmpeg can decode the blob (timeslice-only chunks are often invalid). */
  private captionStopSegmentTimer: number | null = null;
  private autoTranslateTimer: number | null = null;
  private translateSub: Subscription | null = null;
  private lastAutoTranslateAtMs = 0;
  private lastAutoTranslateKey = '';
  private localMicMonitorStream: MediaStream | null = null;
  private speakerMonitorCtx: AudioContext | null = null;
  private speakerMonitorAnalyser: AnalyserNode | null = null;
  private speakerMonitorTimer: number | null = null;
  private speakerActiveUntilMs = 0;

  private scrollTextareaToBottom(el?: ElementRef<HTMLTextAreaElement>): void {
    const area = el?.nativeElement;
    if (!area) {
      return;
    }
    requestAnimationFrame(() => {
      area.scrollTop = area.scrollHeight;
    });
  }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly appointmentService: AppointmentService,
    private readonly userService: UserService,
    private readonly whisperAi: WhisperAiService,
    private readonly sanitizer: DomSanitizer,
    readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(id)) {
      this.back();
      return;
    }
    this.appointmentService.getById(id).subscribe({
      next: (a) => {
        this.appointment = a;
        this.tele = a.teleconsultation;
        this.loading = false;
        this.seedRescheduleFields(a);
        this.refreshParticipantNames(a);
        if (a.type === 'TELECONSULTATION' && !this.tele) {
          this.refreshTele(id);
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Not found';
        this.loading = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.stopLiveCaptions();
    this.closeInlineMeeting();
  }

  get minDateStr(): string {
    return minDateInputYmd();
  }

  get canReschedule(): boolean {
    const a = this.appointment;
    if (!a || (a.status !== 'PENDING' && a.status !== 'CONFIRMED')) {
      return false;
    }
    return this.isPatient || this.isDoctor;
  }

  get schedulingWindowError(): string | null {
    return validateAppointmentScheduling(this.rescheduleDate, this.rescheduleTime);
  }

  /** Prefill date/time from the appointment (no calendar API until panel opens). */
  private seedRescheduleFields(a: AppointmentResponse): void {
    const ymd = appointmentDateToYmd(a.appointmentDate) ?? '';
    this.rescheduleDate = ymd.length >= 10 ? ymd.slice(0, 10) : '';
    this.rescheduleTime = a.timeSlot;
    if (this.rescheduleDate.length >= 10) {
      const y = +this.rescheduleDate.slice(0, 4);
      const m = +this.rescheduleDate.slice(5, 7);
      if (Number.isFinite(y) && m >= 1 && m <= 12) {
        this.rescheduleCalYear = y;
        this.rescheduleCalMonth = m;
      }
    }
  }

  openReschedulePanel(): void {
    if (!this.appointment || !this.canReschedule) {
      return;
    }
    this.showReschedulePanel = true;
    this.calendarLoadError = '';
    this.seedRescheduleFields(this.appointment);
    this.loadRescheduleHints();
  }

  closeReschedulePanel(): void {
    this.showReschedulePanel = false;
  }

  private pad(n: number): string {
    return String(n).padStart(2, '0');
  }

  private monthRange(y: number, m: number): { from: string; to: string } {
    const from = new Date(y, m - 1, 1);
    const to = new Date(y, m, 0);
    const fmt = (d: Date): string =>
      `${d.getFullYear()}-${this.pad(d.getMonth() + 1)}-${this.pad(d.getDate())}`;
    return { from: fmt(from), to: fmt(to) };
  }

  loadRescheduleHints(): void {
    const a = this.appointment;
    if (!a) {
      return;
    }
    const { from, to } = this.monthRange(this.rescheduleCalYear, this.rescheduleCalMonth);
    this.loadingRescheduleCal = true;
    this.calendarLoadError = '';
    const opts: { doctorUserId?: number; patientUserId?: number; excludeAppointmentId?: number } = {
      excludeAppointmentId: a.id
    };
    if (this.authService.hasRole('PATIENT')) {
      opts.doctorUserId = a.doctorUserId;
    } else if (this.authService.hasRole('DOCTOR')) {
      opts.patientUserId = a.patientUserId;
    }
    this.appointmentService.getCalendarHints(from, to, opts).subscribe({
      next: (rows) => {
        this.rescheduleBusy = rows;
        this.loadingRescheduleCal = false;
      },
      error: (err) => {
        this.rescheduleBusy = [];
        this.loadingRescheduleCal = false;
        this.calendarLoadError =
          err?.error?.message || err?.message || 'Could not load busy times.';
      }
    });
  }

  onRescheduleDateChange(value: string): void {
    this.rescheduleDate = value;
    if (value && value.length >= 10) {
      const y = +value.slice(0, 4);
      const m = +value.slice(5, 7);
      if (y > 0 && m >= 1 && m <= 12) {
        if (this.rescheduleCalYear !== y || this.rescheduleCalMonth !== m) {
          this.rescheduleCalYear = y;
          this.rescheduleCalMonth = m;
        }
        this.loadRescheduleHints();
      }
    }
  }

  onRescheduleCalMonthChange(ev: { year: number; month: number }): void {
    this.rescheduleCalYear = ev.year;
    this.rescheduleCalMonth = ev.month;
    this.loadRescheduleHints();
  }

  hasRescheduleSlotConflict(): boolean {
    if (this.schedulingWindowError || this.rescheduleDate.length < 10) {
      return false;
    }
    return hasAppointmentTimeOverlap(
      this.rescheduleDate,
      this.rescheduleTime,
      this.rescheduleBusy,
      APPOINTMENT_SLOT_DURATION_MINUTES
    );
  }

  submitReschedule(): void {
    const a = this.appointment;
    if (!a || !this.canReschedule) {
      return;
    }
    const err = validateAppointmentScheduling(this.rescheduleDate, this.rescheduleTime);
    if (err) {
      this.errorMessage = err;
      return;
    }
    if (this.hasRescheduleSlotConflict()) {
      this.errorMessage = 'That time overlaps another visit. Pick a different slot.';
      return;
    }
    this.rescheduleSubmitting = true;
    this.errorMessage = '';
    const body = {
      appointmentDate: this.rescheduleDate.slice(0, 10),
      timeSlot: this.rescheduleTime.trim()
    };
    this.appointmentService.reschedule(a.id, body).subscribe({
      next: (updated) => {
        this.appointment = updated;
        this.tele = updated.teleconsultation;
        this.rescheduleSubmitting = false;
        this.seedRescheduleFields(updated);
        this.showReschedulePanel = false;
      },
      error: (err) => {
        this.rescheduleSubmitting = false;
        this.errorMessage =
          err.error?.message || err.error?.error || err.message || 'Failed to reschedule';
      }
    });
  }

  private refreshParticipantNames(a: AppointmentResponse): void {
    this.userService.lookupNamesByIds([a.patientUserId, a.doctorUserId]).subscribe({
      next: (rows) => {
        this.nameByUserId.clear();
        for (const r of rows) {
          this.nameByUserId.set(r.id, r);
        }
      },
      error: () => {
        /* keep server-provided names or dashes */
      }
    });
  }

  private refreshTele(appointmentId: number): void {
    this.appointmentService.getTeleconsultation(appointmentId).subscribe({
      next: (t) => {
        this.tele = t;
      },
      error: () => {
        /* no tele yet */
      }
    });
  }

  get patientDisplayName(): string {
    if (!this.appointment) {
      return '—';
    }
    const row = this.nameByUserId.get(this.appointment.patientUserId);
    if (row) {
      return formatUserLookupName(row);
    }
    return appointmentParticipantDisplayName(this.appointment, 'patient');
  }

  get doctorDisplayName(): string {
    if (!this.appointment) {
      return '—';
    }
    const row = this.nameByUserId.get(this.appointment.doctorUserId);
    if (row) {
      return formatUserLookupName(row);
    }
    return appointmentParticipantDisplayName(this.appointment, 'doctor');
  }

  get isDoctor(): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && this.appointment != null && this.appointment.doctorUserId === uid;
  }

  get isPatient(): boolean {
    const uid = this.authService.getCurrentUser()?.userId;
    return uid != null && this.appointment != null && this.appointment.patientUserId === uid;
  }

  /** Only embed Jitsi links we generated on meet.jit.si (same origin as backend room URL). */
  private buildSafeMeetingUrl(url: string): SafeResourceUrl | null {
    if (!url.startsWith(AppointmentDetailComponent.JITSI_ORIGIN)) {
      return null;
    }
    const withParams =
      url +
      (url.includes('#') ? '&' : '#') +
      'config.prejoinPageEnabled=false&config.startWithAudioMuted=false&config.startWithVideoMuted=false';
    return this.sanitizer.bypassSecurityTrustResourceUrl(withParams);
  }

  openInlineMeeting(url: string): void {
    const safe = this.buildSafeMeetingUrl(url);
    if (!safe) {
      this.errorMessage = 'Meeting URL is not a supported in-app Jitsi link.';
      return;
    }
    this.safeMeetingUrl = safe;
    this.inlineMeetingOpen = true;
  }

  closeInlineMeeting(): void {
    this.inlineMeetingOpen = false;
    this.safeMeetingUrl = null;
    this.stopLiveCaptions();
  }

  startVideo(): void {
    if (!this.appointment || this.appointment.status === 'COMPLETED' || this.appointment.status === 'CANCELLED') {
      return;
    }
    this.appointmentService.startTeleconsultation(this.appointment.id).subscribe({
      next: (t) => {
        this.tele = t;
        if (t.meetingUrl) {
          this.openInlineMeeting(t.meetingUrl);
        }
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Could not start teleconsultation';
      }
    });
  }

  openRoom(): void {
    if (
      this.appointment?.status === 'COMPLETED' ||
      this.appointment?.status === 'CANCELLED' ||
      !this.tele?.meetingUrl
    ) {
      return;
    }
    this.openInlineMeeting(this.tele.meetingUrl);
  }

  async toggleLiveCaptions(): Promise<void> {
    if (this.captionsActive) {
      this.stopLiveCaptions();
      return;
    }
    this.captionError = '';
    try {
      await this.acquireCaptionInputStream();
    } catch {
      if (!this.captionError) {
        this.captionError = 'Could not start audio capture. Check permissions or try turning off conference audio.';
      }
      this.releaseCaptionStreams();
      return;
    }
    this.captionsActive = true;
    await this.initLocalSpeakerMonitor();
    this.beginCaptionSegmentLoop();
  }

  /**
   * Prefer tab / conference capture (mixed Jitsi audio). Falls back to microphone if tab audio is missing.
   */
  private async acquireCaptionInputStream(): Promise<void> {
    this.releaseCaptionStreams();
    const md = navigator.mediaDevices;
    if (!md?.getUserMedia) {
      this.captionError = 'Audio capture is not supported in this browser.';
      throw new Error('unsupported');
    }

    if (this.captionUseConferenceAudio && 'getDisplayMedia' in md) {
      try {
        const constraints: Record<string, unknown> = {
          video: true,
          audio: true,
          preferCurrentTab: true,
          selfBrowserSurface: 'include'
        };
        const surface = await (md as MediaDevices & { getDisplayMedia(c?: unknown): Promise<MediaStream> }).getDisplayMedia(
          constraints
        );
        this.conferenceSurfaceStream = surface;
        const audioTracks = surface.getAudioTracks();
        if (audioTracks.length === 0) {
          surface.getTracks().forEach((t) => t.stop());
          this.conferenceSurfaceStream = null;
          this.mediaStream = await md.getUserMedia({ audio: true });
          this.captionError =
            'Tab audio was not shared — using your microphone only. Stop captions, start again, and enable “Share tab audio” in the picker to transcribe the other participant.';
          return;
        }
        this.mediaStream = new MediaStream(audioTracks.slice());
        return;
      } catch {
        this.releaseCaptionStreams();
        if (this.captionUseConferenceAudio) {
          this.captionError =
            'Tab or window share was cancelled or blocked. Uncheck “Conference / tab audio” to use the microphone only.';
          throw new Error('display-media');
        }
      }
    }

    this.mediaStream = await md.getUserMedia({ audio: true });
  }

  private releaseCaptionStreams(): void {
    if (this.speakerMonitorTimer != null) {
      cancelAnimationFrame(this.speakerMonitorTimer);
      this.speakerMonitorTimer = null;
    }
    if (this.speakerMonitorCtx) {
      void this.speakerMonitorCtx.close();
      this.speakerMonitorCtx = null;
    }
    this.speakerMonitorAnalyser = null;
    this.speakerActiveUntilMs = 0;
    if (this.localMicMonitorStream) {
      for (const t of this.localMicMonitorStream.getTracks()) {
        t.stop();
      }
      this.localMicMonitorStream = null;
    }
    if (this.conferenceSurfaceStream) {
      for (const t of this.conferenceSurfaceStream.getTracks()) {
        t.stop();
      }
      this.conferenceSurfaceStream = null;
    }
    if (this.mediaStream) {
      for (const t of this.mediaStream.getTracks()) {
        t.stop();
      }
      this.mediaStream = null;
    }
  }

  private pickCaptionMime(): string {
    if (typeof MediaRecorder === 'undefined' || !MediaRecorder.isTypeSupported) {
      return 'audio/webm';
    }
    if (MediaRecorder.isTypeSupported('audio/webm;codecs=opus')) {
      return 'audio/webm;codecs=opus';
    }
    if (MediaRecorder.isTypeSupported('audio/webm')) {
      return 'audio/webm';
    }
    if (MediaRecorder.isTypeSupported('audio/mp4')) {
      return 'audio/mp4';
    }
    return 'audio/webm';
  }

  private captionFilenameForMime(mime: string): string {
    return mime.includes('mp4') ? 'captions.m4a' : 'captions.webm';
  }

  private async initLocalSpeakerMonitor(): Promise<void> {
    this.speakerMonitorAnalyser = null;
    this.speakerActiveUntilMs = 0;
    if (!this.suppressOwnSpeechEnabled || !this.captionUseConferenceAudio) {
      return;
    }
    const md = navigator.mediaDevices;
    if (!md?.getUserMedia) {
      return;
    }
    try {
      this.localMicMonitorStream = await md.getUserMedia({
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true
        }
      });
      const Ctor = window.AudioContext || (window as typeof window & { webkitAudioContext?: typeof AudioContext }).webkitAudioContext;
      if (!Ctor) {
        return;
      }
      this.speakerMonitorCtx = new Ctor();
      const source = this.speakerMonitorCtx.createMediaStreamSource(this.localMicMonitorStream);
      const analyser = this.speakerMonitorCtx.createAnalyser();
      analyser.fftSize = 1024;
      analyser.smoothingTimeConstant = 0.25;
      source.connect(analyser);
      this.speakerMonitorAnalyser = analyser;
      this.runSpeakerMonitorLoop();
    } catch {
      // If mic monitor is blocked, continue captions without own-speech suppression.
      this.speakerMonitorAnalyser = null;
    }
  }

  private runSpeakerMonitorLoop(): void {
    const analyser = this.speakerMonitorAnalyser;
    if (!analyser || !this.captionsActive) {
      this.speakerMonitorTimer = null;
      return;
    }
    const data = new Uint8Array(analyser.fftSize);
    const tick = (): void => {
      if (!this.captionsActive || !this.speakerMonitorAnalyser) {
        this.speakerMonitorTimer = null;
        return;
      }
      this.speakerMonitorAnalyser.getByteTimeDomainData(data);
      let sum = 0;
      for (let i = 0; i < data.length; i += 1) {
        const centered = (data[i] - 128) / 128;
        sum += centered * centered;
      }
      const rms = Math.sqrt(sum / data.length);
      if (rms > 0.055) {
        this.speakerActiveUntilMs = Date.now() + 800;
      }
      this.speakerMonitorTimer = requestAnimationFrame(tick);
    };
    this.speakerMonitorTimer = requestAnimationFrame(tick);
  }

  private isLocalSpeakerActive(): boolean {
    if (!this.suppressOwnSpeechEnabled || !this.captionUseConferenceAudio) {
      return false;
    }
    return Date.now() < this.speakerActiveUntilMs;
  }

  /**
   * Record short segments by stopping MediaRecorder on a timer so each blob is a valid container
   * (Chrome/WebM timeslice blobs are often not decodable standalone).
   */
  private beginCaptionSegmentLoop(): void {
    const stream = this.mediaStream;
    if (!stream || !this.captionsActive) {
      return;
    }
    const mime = this.pickCaptionMime();
    const filename = this.captionFilenameForMime(mime);
    const segmentMs = 4500;

    const spin = (): void => {
      if (!this.captionsActive || !stream.active) {
        return;
      }
      const chunks: Blob[] = [];
      let rec: MediaRecorder;
      try {
        rec = new MediaRecorder(stream, { mimeType: mime });
      } catch {
        this.captionError = 'Recording format not supported in this browser.';
        this.stopLiveCaptions();
        return;
      }
      this.mediaRecorder = rec;
      rec.ondataavailable = (ev: BlobEvent) => {
        if (ev.data?.size) {
          chunks.push(ev.data);
        }
      };
      rec.onstop = () => {
        const blob = new Blob(chunks, { type: mime });
        if (blob.size > 800 && this.captionsActive) {
          this.uploadAudioChunk(blob, filename);
        }
        if (this.captionsActive && stream.active) {
          window.setTimeout(() => spin(), 120);
        }
      };
      rec.start();
      if (this.captionStopSegmentTimer != null) {
        clearTimeout(this.captionStopSegmentTimer);
      }
      this.captionStopSegmentTimer = window.setTimeout(() => {
        this.captionStopSegmentTimer = null;
        if (rec.state === 'recording') {
          rec.stop();
        }
      }, segmentMs);
    };

    spin();
  }

  private uploadAudioChunk(blob: Blob, filename: string): void {
    if (this.isLocalSpeakerActive()) {
      return;
    }
    this.captionBusy = true;
    this.whisperAi.transcribeChunk(blob, filename, this.speechSourceLang).subscribe({
      next: (r) => {
        this.captionBusy = false;
        if (r.text?.trim()) {
          this.liveTranscript = this.liveTranscript
            ? `${this.liveTranscript}\n${r.text.trim()}`
            : r.text.trim();
          this.scheduleAutoTranslate();
        }
      },
      error: (err) => {
        this.captionBusy = false;
        const raw = err.error;
        let detail: string | undefined;
        if (raw && typeof raw === 'object' && 'detail' in raw) {
          const d = (raw as { detail: unknown }).detail;
          detail = Array.isArray(d) ? JSON.stringify(d) : String(d);
        }
        this.captionError =
          detail || (raw && typeof raw === 'object' && 'message' in raw
            ? String((raw as { message: unknown }).message)
            : undefined) ||
          err.message ||
          'Transcription failed';
      }
    });
  }

  stopLiveCaptions(): void {
    this.captionsActive = false;
    if (this.captionStopSegmentTimer != null) {
      clearTimeout(this.captionStopSegmentTimer);
      this.captionStopSegmentTimer = null;
    }
    if (this.autoTranslateTimer != null) {
      clearTimeout(this.autoTranslateTimer);
      this.autoTranslateTimer = null;
    }
    if (this.translateSub) {
      this.translateSub.unsubscribe();
      this.translateSub = null;
    }
    if (this.mediaRecorder && this.mediaRecorder.state !== 'inactive') {
      try {
        this.mediaRecorder.stop();
      } catch {
        /* ignore */
      }
    }
    this.mediaRecorder = null;
    this.releaseCaptionStreams();
  }

  private scheduleAutoTranslate(): void {
    if (!this.autoTranslateEnabled) {
      return;
    }
    const text = this.liveTranscript.trim();
    if (!text) {
      return;
    }
    if (this.autoTranslateTimer != null) {
      clearTimeout(this.autoTranslateTimer);
    }
    this.autoTranslateTimer = window.setTimeout(() => {
      this.autoTranslateTimer = null;
      if (!this.autoTranslateEnabled || !this.liveTranscript.trim()) {
        return;
      }
      const src = this.autoTranslateSourceText();
      const key = `${this.translateTargetLang}|${src}`;
      const now = Date.now();
      // Quota protection: skip repeated payload and limit call frequency.
      if (key === this.lastAutoTranslateKey) {
        return;
      }
      if (now - this.lastAutoTranslateAtMs < 2500) {
        this.scheduleAutoTranslate();
        return;
      }
      this.translateTranscript(false);
    }, 900);
  }

  runTranslate(): void {
    this.translateTranscript(true);
  }

  onTranslateTargetLangChange(): void {
    if (!this.liveTranscript.trim()) {
      return;
    }
    if (this.autoTranslateEnabled) {
      this.translateTranscript(false);
    }
  }

  private autoTranslateSourceText(): string {
    const full = this.liveTranscript.trim();
    const maxChars = 1600;
    if (full.length <= maxChars) {
      return full;
    }
    const cut = full.slice(full.length - maxChars);
    const nl = cut.indexOf('\n');
    return nl >= 0 ? cut.slice(nl + 1).trim() : cut.trim();
  }

  private translateTranscript(isManual: boolean): void {
    const src = isManual ? this.liveTranscript.trim() : this.autoTranslateSourceText();
    if (!src) {
      if (isManual) {
        this.captionError = 'Nothing to translate yet — start live captions first.';
      }
      return;
    }
    if (isManual) {
      this.captionError = '';
    }
    if (this.translateSub) {
      this.translateSub.unsubscribe();
      this.translateSub = null;
    }
    this.translateBusy = true;
    this.translateSub = this.whisperAi.translate(src, this.translateTargetLang).subscribe({
      next: (r) => {
        this.translateBusy = false;
        this.translateSub = null;
        this.captionError = '';
        if (!isManual) {
          this.lastAutoTranslateAtMs = Date.now();
          this.lastAutoTranslateKey = `${this.translateTargetLang}|${src}`;
        }
        this.translatedText = r.translated_text;
        this.scrollTextareaToBottom(this.translatedArea);
      },
      error: (err) => {
        this.translateBusy = false;
        this.translateSub = null;
        const msg =
          err.error?.detail || err.error?.message || err.message || 'Translation request failed';
        const msgStr = typeof msg === 'string' ? msg : String(msg);
        if (msgStr.includes('ApiQuotaExceededError') || msgStr.includes('api_translation_chars')) {
          this.autoTranslateEnabled = false;
          this.captionError =
            'Translation quota exceeded for Lara API. Auto-translate has been paused.';
          return;
        }
        this.captionError = isManual
          ? msgStr
          : `Translation (auto): ${msgStr}`;
      }
    });
  }

  complete(): void {
    if (!this.appointment) {
      return;
    }
    this.appointmentService.complete(this.appointment.id).subscribe({
      next: (a) => {
        this.appointment = a;
        this.tele = a.teleconsultation;
        this.refreshParticipantNames(a);
        this.closeInlineMeeting();
      },
      error: (err) => {
        this.errorMessage = err.error?.error || 'Could not complete';
      }
    });
  }

  back(): void {
    const dest = this.router.url.split('?')[0].includes('/admin/appointments')
      ? '/admin/appointments'
      : '/appointments';
    const qp = this.route.snapshot.queryParams;
    this.router.navigate([dest], {
      queryParams: Object.keys(qp).length ? qp : undefined
    });
  }
}
