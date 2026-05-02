import { Component, OnInit } from '@angular/core';
import html2canvas from 'html2canvas';
import { FormArray, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PrescriptionService } from '../../../core/services/prescription.service';
import { MedicineService } from '../../../core/services/medicine.service';
import { MedicalRecordService } from '../../../core/services/medical-record.service';
import { PrescriptionItemRequest, PrescriptionRequest } from '../../../models/prescription.model';
import { Medicine, OpenFDAMedicine } from '../../../models/medicine.model';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';
import { NotificationService } from '../../../shared/services/notification.service';
import { getParamFromRouteTree } from '../../../shared/utils/route-params';

@Component({
  selector: 'app-prescription-form',
  templateUrl: './prescription-form.component.html',
  styleUrls: ['./prescription-form.component.scss']
})
export class PrescriptionFormComponent implements OnInit {
  patientId: number | null = null;
  recordId: number | null = null;
  prescriptionId: number | null = null;
  isEdit = false;
  loading = false;
  saving = false;

  medicines: Medicine[] = [];
  aiRecommendations: string[] = []; // AI Drug Recs
  recordDiagnosis: string | null = null;

  searchResults: { [index: number]: OpenFDAMedicine[] } = {};
  medicineWarnings: { [index: number]: string } = {};
  searchSubject = new Subject<{ index: number; query: string }>();

  readonly form = this.fb.group({
    status: ['ACTIVE', [Validators.required]],
    items: this.fb.array<FormGroup>([this.createItemGroup()])
  });

  constructor(
    private readonly fb: FormBuilder,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly prescriptionService: PrescriptionService,
    private readonly medicineService: MedicineService,
    private readonly medicalRecordService: MedicalRecordService,
    private readonly notification: NotificationService
  ) {}

  get itemsArray(): FormArray<FormGroup> {
    return this.form.controls.items;
  }

  addItem(): void {
    this.itemsArray.push(this.createItemGroup());
  }

  removeItem(index: number): void {
    if (this.itemsArray.length <= 1) return;
    this.itemsArray.removeAt(index);
  }

  private createItemGroup(initial?: {
    medicineId?: number;
    medicineNameSearch?: string;
    dosage?: string;
    frequency?: string;
    quantity?: number;
    startDate?: string;
    endDate?: string;
    instructions?: string;
    isAiRecommended?: boolean;
  }): FormGroup {
    return this.fb.group({
      medicineNameSearch: [initial?.medicineNameSearch ?? ''],
      medicineId: [initial?.medicineId ?? '', [Validators.required]],
      dosage: [initial?.dosage ?? '', [Validators.required, Validators.maxLength(50)]],
      frequency: [initial?.frequency ?? '', [Validators.required, Validators.maxLength(50)]],
      quantity: [initial?.quantity ?? 1, [Validators.required, Validators.min(1)]],
      startDate: [initial?.startDate ?? '', [Validators.required]],
      endDate: [initial?.endDate ?? ''],
      instructions: [initial?.instructions ?? '', [Validators.maxLength(500)]],
      isAiRecommended: [initial?.isAiRecommended ?? false]
    });
  }

  ngOnInit(): void {
    const pid = getParamFromRouteTree(this.route, 'patientId');
    const rid = getParamFromRouteTree(this.route, 'recordId');
    if (!pid || !rid) {
      this.router.navigate(['/patients']);
      return;
    }
    this.patientId = +pid;
    this.recordId = +rid;

    this.loadMedicines();
    this.loadRecordForAi(this.recordId);

    const path = this.route.snapshot.routeConfig?.path;
    if (path === 'new') {
      this.isEdit = false;
    } else if (path === ':id/edit') {
      this.isEdit = true;
      const id = this.route.snapshot.paramMap.get('id');
      if (id) {
        this.prescriptionId = +id;
        this.loadPrescription(this.prescriptionId);
      }
    }

    this.searchSubject.pipe(debounceTime(500)).subscribe(({ index, query }) => {
      if (query.trim().length < 2) {
        this.searchResults[index] = [];
        return;
      }
      this.medicineService.searchOpenFda(query).subscribe({
        next: (res) => (this.searchResults[index] = res),
        error: () => (this.searchResults[index] = [])
      });
    });
  }

  private loadMedicines(): void {
    this.medicineService.getAll().subscribe({
      next: (list) => (this.medicines = list),
      error: () => this.notification.error('Impossible de charger les médicaments')
    });
  }

  private loadRecordForAi(recordId: number): void {
    this.medicalRecordService.getRecordById(recordId).subscribe({
      next: (record) => {
        if (record.diagnosis) {
          this.recordDiagnosis = record.diagnosis;
          this.fetchAiRecommendations(record.diagnosis);
        }
      }
    });
  }

  private fetchAiRecommendations(diagnosis: string): void {
    this.prescriptionService.recommendDrugs(diagnosis).subscribe({
      next: (res) => {
        if (res.recommended_drugs && res.recommended_drugs.length > 0) {
          this.aiRecommendations = res.recommended_drugs;
        }
      },
      error: () => {
        // Silently fail if AI is down
        console.warn('AI Drug Recommender could not be reached.');
      }
    });
  }

  applyAiRecommendation(itemIndex: number, drug: string): void {
    // Fill the visual input + mark as AI recommended
    this.itemsArray.at(itemIndex).patchValue({ medicineNameSearch: drug, isAiRecommended: true });
    // Trigger OpenFDA search to get the official ID
    this.searchSubject.next({ index: itemIndex, query: drug });
    this.notification.info('Checking OpenFDA for ' + drug + '...');
  }


  private loadPrescription(id: number): void {
    this.loading = true;
    this.prescriptionService.getPrescriptionById(id).subscribe({
      next: (p) => {
        this.form.patchValue({ status: p.status || 'ACTIVE' });
        this.itemsArray.clear();
        const items = p.items?.length ? p.items : [];
        if (!items.length) {
          this.itemsArray.push(this.createItemGroup());
        } else {
          items.forEach(item =>
            this.itemsArray.push(this.createItemGroup({
              medicineId: item.medicine?.id,
              medicineNameSearch: item.medicine?.name ?? '',
              dosage: item.dosage ?? '',
              frequency: item.frequency ?? '',
              quantity: item.quantity ?? 1,
              startDate: item.startDate ? item.startDate.substring(0, 10) : '',
              endDate: item.endDate ? item.endDate.substring(0, 10) : '',
              instructions: item.instructions ?? ''
            }))
          );
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        if (this.patientId != null && this.recordId != null) {
          this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
        }
      }
    });
  }

  onSearchInput(index: number, event: Event): void {
    const query = (event.target as HTMLInputElement).value;
    // Clear id if user types something new
    const ctrl = this.itemsArray.at(index).get('medicineId');
    if (ctrl?.value) {
      ctrl.patchValue('');
      this.medicineWarnings[index] = '';
    }
    this.searchSubject.next({ index, query });
  }

  selectMedicine(index: number, med: OpenFDAMedicine): void {
    this.medicineWarnings[index] = med.sideEffects || '';
    this.itemsArray.at(index).patchValue({ medicineNameSearch: med.name });
    
    this.medicineService.getOrCreate({ 
      name: med.name, 
      description: med.description, 
      sideEffects: med.sideEffects 
    }).subscribe({
      next: (localMed) => {
        this.itemsArray.at(index).patchValue({ medicineId: localMed.id });
        this.searchResults[index] = [];
      },
      error: () => this.notification.error('Erreur lors de la sauvegarde du médicament')
    });
  }

  async submit(): Promise<void> {
    if (this.form.invalid || this.patientId == null || this.recordId == null) {
      this.form.markAllAsTouched();
      return;
    }

    const v = this.form.getRawValue();
    const rawItems = (v.items ?? []) as Array<Record<string, unknown>>;
    const items: PrescriptionItemRequest[] = rawItems
      .filter(m => m['medicineId'])
      .map(m => ({
        medicineId: Number(m['medicineId']),
        dosage: String(m['dosage'] ?? '').trim(),
        frequency: String(m['frequency'] ?? '').trim(),
        quantity: Number(m['quantity'] ?? 1),
        startDate: String(m['startDate'] ?? ''),
        endDate: m['endDate'] ? String(m['endDate']) : null,
        instructions: String(m['instructions'] ?? '').trim() || null,
        isAiRecommended: !!m['isAiRecommended']
      }));

    if (!items.length) {
      this.notification.error('Ajoute au moins un médicament.');
      return;
    }

    this.saving = true;

    // Cloudinary Image Capture
    let imageBase64 = '';
    try {
      // Pour éviter les coupures, on force la hauteur au maximum
      const captureElem = document.getElementById('capture-ticket') as HTMLElement;
      if (captureElem) {
        
        const canvas = await html2canvas(captureElem, { 
          scale: 2, // Haute résolution
          useCORS: true,
          backgroundColor: '#ffffff'
        });
        imageBase64 = canvas.toDataURL('image/png');
      }
    } catch (e) {
      console.warn('UI Capture failed. The prescription will save without an image.', e);
    }

    const body: PrescriptionRequest = {
      items,
      status: String(v.status ?? 'ACTIVE'),
      medicalRecordId: this.recordId,
      patientId: this.patientId,
      imageBase64: imageBase64 || undefined
    };

    const req$ = this.isEdit && this.prescriptionId
      ? this.prescriptionService.updatePrescription(this.prescriptionId, body)
      : this.prescriptionService.createPrescription(body);

    req$.subscribe({
      next: () => {
        this.notification.success(this.isEdit ? 'Prescription mise à jour' : 'Prescription créée');
        this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
        this.saving = false;
      },
      error: () => {
        this.saving = false;
      }
    });
  }

  cancel(): void {
    if (this.patientId != null && this.recordId != null) {
      this.router.navigate(['/patients', this.patientId, 'records', this.recordId, 'prescriptions']);
    }
  }
}
