import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { AuthService } from '../../../core/services/auth.service';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { Icd10Service, Icd10Result } from '../../../core/services/icd10.service';
import { MedicalRecordRequest } from '../../../models/medical-record.model';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-record-form',
  templateUrl: './record-form.component.html',
  styleUrls: ['./record-form.component.scss']
})
export class RecordFormComponent implements OnInit {
  isListening = false;

  patientId: number | null = null;
  recordId: number | null = null;
  isEdit = false;
  loading = false;
  saving = false;

  // ICD-10 autocomplete
  icdResults: Icd10Result[] = [];
  icdSearching = false;
  showIcdDropdown = false;
  private readonly diagnosisSearch$ = new Subject<string>();

  readonly form = this.fb.group({
    diagnosis: ['', [Validators.required, Validators.maxLength(255)]],
    notes: ['', [Validators.maxLength(2000)]],
    date: ['', [Validators.required]],
    severity: ['MEDIUM', [Validators.required]],
    status: ['ACTIVE', [Validators.required]]
  });

  readonly severities = ['LOW', 'MEDIUM', 'HIGH'] as const;
  readonly statuses = ['ACTIVE', 'CLOSED'] as const;

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly recordService: MedicalRecordService,
    private readonly auth: AuthService,
    private readonly notification: NotificationService,
    private readonly icd10Service: Icd10Service
  ) {}

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    if (!pid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'new') {
      this.isEdit = false;
    } else if (path === ':recordId/edit') {
      this.isEdit = true;
      const rid = this.route.snapshot.paramMap.get('recordId');
      if (rid) {
        this.recordId = +rid;
        this.loadRecord(this.recordId);
      }
    }

    // ICD-10 autocomplete pipeline
    this.diagnosisSearch$
      .pipe(
        debounceTime(400),
        distinctUntilChanged(),
        switchMap((term) => {
          if (term.length < 2) {
            this.icdResults = [];
            this.icdSearching = false;
            this.showIcdDropdown = false;
            return [];
          }
          this.icdSearching = true;
          return this.icd10Service.search(term);
        })
      )
      .subscribe((results) => {
        this.icdResults = results;
        this.icdSearching = false;
        this.showIcdDropdown = results.length > 0;
      });
  }

  onDiagnosisInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.diagnosisSearch$.next(value);
  }

  selectDiagnosis(result: Icd10Result): void {
    this.form.patchValue({ diagnosis: `${result.code} - ${result.name}` });
    this.showIcdDropdown = false;
    this.icdResults = [];
  }

  hideIcdDropdown(): void {
    // Delay to allow click on dropdown item to register
    setTimeout(() => {
      this.showIcdDropdown = false;
    }, 200);
  }

  aiPredicting = false;
  
  autoPredictSeverity(): void {
    const diagnosis = this.form.get('diagnosis')?.value;
    if (!diagnosis || diagnosis.trim() === '') {
      this.notification.error("Please enter a diagnosis first.");
      return;
    }

    this.aiPredicting = true;
    this.recordService.predictSeverity(diagnosis).subscribe({
      next: (result) => {
        this.form.patchValue({ severity: result.severity });
        const confPercent = Math.round(result.confidence * 100);
        this.notification.success(`AI predicted: ${result.severity} (${confPercent}% confidence)`);
        this.aiPredicting = false;
      },
      error: () => {
        this.notification.error("Failed to get AI prediction");
        this.aiPredicting = false;
      }
    });
  }

  startDictation(): void {
    if (this.isListening) return;

    // Detect browser support for Web Speech API
    const SpeechRecognitionAPI = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognitionAPI) {
      this.notification.error("This browser does not support Speech Recognition. Try Chrome or Edge.");
      return;
    }

    const recognition = new SpeechRecognitionAPI();
    recognition.lang = 'en-US'; // Set to 'fr-FR' if you prefer French detection
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    recognition.onstart = () => {
      this.isListening = true;
      // Optional UI trigger since it updates state
    };

    recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      const currentNotes = this.form.get('notes')?.value || '';
      
      // Capitalize first letter of transcript
      const formattedTranscript = transcript.charAt(0).toUpperCase() + transcript.slice(1);
      
      const newNotes = currentNotes.trim() ? `${currentNotes.trim()}\n${formattedTranscript}.` : `${formattedTranscript}.`;
      this.form.patchValue({ notes: newNotes });
      this.notification.success("Dictation added to notes!");
    };

    recognition.onerror = (event: any) => {
      this.isListening = false;
      if (event.error !== 'no-speech') {
        this.notification.error("Microphone error: " + event.error);
      }
    };

    recognition.onend = () => {
      this.isListening = false;
    };

    recognition.start();
  }

  private loadRecord(id: number): void {
    this.loading = true;
    this.recordService.getRecordById(id).subscribe({
      next: (r) => {
        this.form.patchValue({
          diagnosis: r.diagnosis,
          notes: r.notes ?? '',
          date: r.date.substring(0, 10),
          severity: r.severity,
          status: r.status
        });
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.router.navigate(['/patients', this.patientId, 'records']);
      }
    });
  }

  submit(): void {
    if (this.form.invalid || this.patientId == null) {
      this.form.markAllAsTouched();
      return;
    }
    const doctorId = this.auth.getCurrentUser()?.userId;
    if (!doctorId) {
      this.notification.error('Impossible de déterminer le médecin (session).');
      return;
    }

    const v = this.form.getRawValue();
    const body: MedicalRecordRequest = {
      diagnosis: v.diagnosis!.trim(),
      notes: v.notes?.trim() || null,
      date: v.date!,
      severity: v.severity as MedicalRecordRequest['severity'],
      status: v.status!.trim(),
      patientId: this.patientId,
      doctorId
    };

    this.saving = true;
    const req$ =
      this.isEdit && this.recordId
        ? this.recordService.updateRecord(this.recordId, body)
        : this.recordService.createRecord(body);

    req$.subscribe({
      next: (rec) => {
        this.notification.success(this.isEdit ? 'Dossier mis à jour' : 'Dossier créé');
        this.router.navigate(['/patients', this.patientId, 'records']);
        this.saving = false;
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.patientId != null) {
      this.router.navigate(['/patients', this.patientId, 'records']);
    }
  }
}
