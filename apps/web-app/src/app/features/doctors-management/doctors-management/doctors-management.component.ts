import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { trigger, transition, style, animate } from '@angular/animations';
import { DoctorService } from '../../../core/services/doctor.service';
import { DoctorVerificationService } from '../../../core/services/doctor-verification.service';
import { WebSocketService } from '../../../core/services/web-socket.service';
import { DoctorResponse } from '../../../shared/models/doctor.model';
import { DoctorVerification } from '../../../shared/models/doctor-verification.model';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { NgZone } from '@angular/core';
import { AuthService } from 'src/app/core/services/auth.service';

@Component({
  selector: 'app-doctors-management',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  providers: [DoctorService],
  templateUrl: './doctors-management.component.html',
  styleUrl: './doctors-management.component.scss',
  animations: [
    trigger('toastAnimation', [
      transition(':enter', [
        style({ transform: 'translateX(400px)', opacity: 0 }),
        animate('300ms ease-out', style({ transform: 'translateX(0)', opacity: 1 }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ transform: 'translateX(400px)', opacity: 0 }))
      ])
    ])
  ]
})
export class DoctorsManagementComponent implements OnInit, OnDestroy {
  doctors: DoctorResponse[] = [];
  isLoading = false;
  error: string | null = null;
  imageCache = new Map<string, SafeUrl>();

  showVerificationModal = false;
  selectedDoctor: DoctorResponse | null = null;
  selectedVerification: DoctorVerification | null = null;
  verificationLoading = false;
  verificationError: string | null = null;

  showImageModal = false;
  selectedImageUrl: SafeUrl | null = null;
  selectedImageTitle = '';

  documentImageCache = new Map<string, SafeUrl>();

  toastMessages: Array<{ message: string; type: 'success' | 'error' | 'warning' }> = [];
  private destroy$ = new Subject<void>();

  constructor(
    private readonly doctorService: DoctorService,
    private readonly doctorVerificationService: DoctorVerificationService,
    private readonly httpClient: HttpClient,
    private readonly sanitizer: DomSanitizer,
    private readonly webSocketService: WebSocketService,
    private readonly ngZone: NgZone,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadDoctors();
    
    // Connect to WebSocket after a short delay to ensure page is loaded
    setTimeout(() => {
      this.initializeWebSocket();
    }, 1000);
  }

  private initializeWebSocket(): void {
    this.webSocketService.connect();
    
    // Listen for new doctors from WebSocket
    this.webSocketService.newDoctor$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (newDoctor: DoctorResponse) => {
          console.log('New doctor received from WebSocket:', newDoctor);
          // Add new doctor to the beginning of the list
          this.doctors = [newDoctor, ...this.doctors];
          this.showToast('A new doctor has been added', 'success');
        },
        error: (err) => {
          console.error('Error receiving new doctor:', err);
        }
      });

    // Listen for new doctor verifications from WebSocket
    this.webSocketService.newVerification$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (verification: DoctorVerification) => {
          console.log('Verification event via WebSocket:', verification);

          this.ngZone.run(() => {
            const isExisting = this.selectedVerification?.verification_id === verification.verification_id;

            if (isExisting) {
              // Update the modal if the current verification matches
              this.selectedVerification = verification;
              this.showToast('A doctor verification has been updated', 'success');
            } else {
              // Otherwise, treat it as a new verification
              this.showToast('A new doctor verification has been added', 'success');
            }

            // Update doctor list if needed
            const doctorIndex = this.doctors.findIndex(d => d.id === verification.doctorId);
            if (doctorIndex !== -1) {
              this.doctors[doctorIndex] = {
                ...this.doctors[doctorIndex],
                isActive: verification.status === 'VERIFIED' ? true : this.doctors[doctorIndex].isActive
              };
            }
          });
        },
        error: (err) => console.error('Error receiving verification event:', err)
      });

    // Listen for contract approvals from WebSocket
    this.webSocketService.contractApproved$
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (verification: DoctorVerification) => {
          console.log('Contract approved event via WebSocket:', verification);

          this.ngZone.run(() => {
            // Check if this is the verification we're currently viewing
            if (this.selectedVerification?.verification_id === verification.verification_id) {
              console.log('Doctor approved contract for current verification!');
              this.selectedVerification = verification;
              this.showToast('Doctor approved the contract! Completing verification...', 'success');
              
              // Auto-complete the verification
              setTimeout(() => {
                this.completeVerification();
              }, 1000);
            }
          });
        },
        error: (err) => console.error('Error receiving contract approval:', err)
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.webSocketService.disconnect();
  }

  onViewVerification(doctor: DoctorResponse): void {
    console.log('onViewVerification called for doctor:', doctor);
    this.selectedDoctor = doctor;
    this.showVerificationModal = true;
    this.verificationLoading = true;
    this.verificationError = null;
    this.selectedVerification = null;

    this.doctorVerificationService.getVerificationByDoctorId(doctor.id).subscribe({
      next: (verification) => {
        console.log('Verification data received:', verification);
        if (verification === null) {
          console.log('No verification record found for doctor:', doctor.id);
          // Check if doctor is active (verified) or hasn't submitted
          if (doctor.isActive) {
            this.verificationError = 'ALREADY_VERIFIED';
          } else {
            this.verificationError = 'NOT_SUBMITTED';
          }
          this.selectedVerification = null;
        } else {
          console.log('Verification ID:', verification.verification_id);
          console.log('Verification status:', verification.status);
          console.log('Verification licenseNumber:', verification.licenseNumber);
          console.log('Verification cv:', verification.cv);
          console.log('Verification diploma:', verification.diploma);
          console.log('Verification nationalId:', verification.nationalId);
          console.log('Verification submittedAt:', verification.submittedAt);
          console.log('Verification Contract Approved:', verification.contractApproved);
          this.selectedVerification = verification;
          
          // If contract is already approved, complete the verification automatically
          if (verification.contractApproved) {
            console.log('Contract already approved! Auto-completing verification...');
            this.verificationLoading = false;
            setTimeout(() => {
              this.completeVerification();
            }, 500);
            return;
          }
        }
        this.verificationLoading = false;
        console.log('selectedVerification set:', this.selectedVerification);
      },
      error: (err) => {
        console.error('Error loading verification:', err);
        this.verificationError = 'Failed to load verification details';
        this.verificationLoading = false;
      }
    });
  }

  closeVerificationModal(): void {
    this.showVerificationModal = false;
    this.selectedVerification = null;
    this.selectedDoctor = null;
  }

  getDocumentImageUrl(documentPath: string | undefined): SafeUrl | null {
    if (!documentPath) {
      return null;
    }

    // Convert backslashes to forward slashes
    const normalizedPath = documentPath.replace(/\\\\/g, '/');

    // Check if already cached
    if (this.documentImageCache.has(normalizedPath)) {
      return this.documentImageCache.get(normalizedPath) || null;
    }

    // Load image through HTTP client (with JWT auth)
    const imageUrl = `http://localhost:8082/${normalizedPath}`;
    this.httpClient.get(imageUrl, { responseType: 'blob' }).subscribe({
      next: (imageBlob) => {
        const objectUrl = URL.createObjectURL(imageBlob);
        const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        this.documentImageCache.set(normalizedPath, safeUrl);
      },
      error: (err) => {
        console.error('Failed to load document image:', imageUrl, err);
      }
    });

    return null;
  }

  openImageModal(documentPath: string | undefined, title: string): void {
    if (!documentPath) return;

    const normalizedPath = documentPath.replace(/\\\\/g, '/');
    const imageUrl = `http://localhost:8082/${normalizedPath}`;

    // Check if cached, if not load it
    if (this.documentImageCache.has(normalizedPath)) {
      this.selectedImageUrl = this.documentImageCache.get(normalizedPath) || null;
      this.selectedImageTitle = title;
      this.showImageModal = true;
    } else {
      // Load and show
      this.httpClient.get(imageUrl, { responseType: 'blob' }).subscribe({
        next: (imageBlob) => {
          const objectUrl = URL.createObjectURL(imageBlob);
          const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
          this.documentImageCache.set(normalizedPath, safeUrl);
          this.selectedImageUrl = safeUrl;
          this.selectedImageTitle = title;
          this.showImageModal = true;
        },
        error: (err) => {
          console.error('Failed to load document image:', imageUrl, err);
        }
      });
    }
  }

  closeImageModal(): void {
    this.showImageModal = false;
    this.selectedImageUrl = null;
    this.selectedImageTitle = '';
  }

  showToast(message: string, type: 'success' | 'error' | 'warning'): void {
    this.toastMessages.push({ message, type });
    // Auto-remove after 4 seconds
    setTimeout(() => {
      this.toastMessages.shift();
    }, 4000);
  }

  approveVerification(): void {
    if (!this.selectedVerification || !this.selectedDoctor) return;

    const verificationId = this.selectedVerification.verification_id;
    const token = this.authService.getToken();
    
    if (!token) {
      this.showToast('Authentication token not found', 'error');
      return;
    }

    // Step 1: Approve the verification (generates token and sends email to doctor)
    this.verificationLoading = true;
    this.doctorVerificationService.approveVerification(verificationId, token).subscribe({
      next: () => {
        console.log('Verification record approved successfully');
        this.showToast('Doctor approval email sent. Waiting for contract approval...', 'success');
        this.verificationLoading = false;
      },
      error: (err) => {
        console.error('Error approving verification:', err);
        this.showToast('Failed to approve verification', 'error');
        this.verificationLoading = false;
      }
    });
  }

  completeVerification(): void {
    if (!this.selectedVerification || !this.selectedDoctor) return;

    const verificationId = this.selectedVerification.verification_id;
    const doctorId = this.selectedDoctor.id;

    console.log('Auto-completing verification for doctor:', doctorId, 'verification:', verificationId);

    // Step 1: Verify the doctor
    this.verificationLoading = true;
    this.doctorService.verifyDoctor(doctorId).subscribe({
      next: () => {
        console.log('Doctor verified successfully');
        
        // Step 2: Delete the verification record
        this.doctorVerificationService.deleteVerification(verificationId).subscribe({
          next: () => {
            console.log('Verification record deleted successfully');
            this.showToast('Doctor verified and contract process completed', 'success');
            this.verificationLoading = false;
            setTimeout(() => {
              this.closeVerificationModal();
              this.loadDoctors();
            }, 1500);
          },
          error: (err) => {
            console.error('Error deleting verification:', err);
            this.showToast('Doctor verified but failed to delete verification record', 'error');
            this.verificationLoading = false;
            setTimeout(() => {
              this.closeVerificationModal();
              this.loadDoctors();
            }, 1500);
          }
        });
      },
      error: (err) => {
        console.error('Error verifying doctor:', err);
        this.showToast('Failed to verify doctor', 'error');
        this.verificationLoading = false;
      }
    });
  }

  rejectVerification(): void {
    if (!this.selectedVerification || !this.selectedDoctor) return;

    const verificationId = this.selectedVerification.verification_id;
    const doctorId = this.selectedDoctor.id;

    // Step 1: Delete the verification record
    this.doctorVerificationService.rejectVerification(verificationId).subscribe({
      next: () => {
        console.log('Verification record rejected successfully');
        this.showToast('Doctor rejected', 'success');
      },
      error: (err) => {
        console.error('Error rejecting verification:', err);
        this.showToast('Failed to reject doctor', 'error');
      }
    });
  }

  deleteVerification(): void {
    if (!this.selectedVerification) return;

    if (!confirm('Are you sure you want to delete this verification record? This action cannot be undone.')) {
      return;
    }

    const verificationId = this.selectedVerification.verification_id;
    this.doctorVerificationService.deleteVerification(verificationId).subscribe({
      next: () => {
        console.log('Verification deleted successfully');
        this.showToast('Verification deleted successfully', 'success');
        setTimeout(() => {
          this.closeVerificationModal();
          this.loadDoctors();
        }, 1500);
      },
      error: (err) => {
        console.error('Error deleting verification:', err);
        this.showToast('Failed to delete verification', 'error');
      }
    });
  }

  deleteUnverifiedDoctor(): void {
    if (!this.selectedDoctor) return;

    const doctorId = this.selectedDoctor.id;
    this.doctorService.deleteDoctor(doctorId).subscribe({
      next: () => {
        console.log('Doctor deleted successfully');
        this.showToast('Doctor deleted successfully', 'success');
        setTimeout(() => {
          this.closeVerificationModal();
          this.loadDoctors();
        }, 1500);
      },
      error: (err) => {
        console.error('Error deleting doctor:', err);
        this.showToast('Failed to delete doctor', 'error');
      }
    });
  }

  getStatusBadgeClass(status: string | undefined): string {
    if (!status) return 'badge-warning';
    switch (status) {
      case 'VERIFIED':
        return 'badge-success';
      case 'REJECTED':
        return 'badge-danger';
      case 'PENDING':
      default:
        return 'badge-warning';
    }
  }

  getStatusLabel(status: string | undefined): string {
    if (!status) return 'Pending';
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  loadDoctors(): void {
    this.isLoading = true;
    this.error = null;
    this.doctorService.getDoctors().subscribe({
      next: (data) => {
        this.doctors = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading doctors:', err);
        this.error = 'Failed to load doctors';
        this.isLoading = false;
      }
    });
  }

  getFullName(doctor: DoctorResponse): string {
    return `Dr. ${doctor.firstName} ${doctor.lastName}`;
  }

  getProfilePictureUrl(doctor: DoctorResponse): SafeUrl | null {
    if (!doctor.profilePictureUrl) {
      return null;
    }

    // Check if already cached
    if (this.imageCache.has(doctor.profilePictureUrl)) {
      return this.imageCache.get(doctor.profilePictureUrl) || null;
    }

    // Load image through HTTP client (with JWT auth)
    const imageUrl = `http://localhost:8082/${doctor.profilePictureUrl}`;
    this.httpClient.get(imageUrl, { responseType: 'blob' }).subscribe({
      next: (imageBlob) => {
        const objectUrl = URL.createObjectURL(imageBlob);
        const safeUrl = this.sanitizer.bypassSecurityTrustUrl(objectUrl);
        this.imageCache.set(doctor.profilePictureUrl, safeUrl);
      },
      error: (err) => {
        console.error('Failed to load image:', imageUrl, err);
      }
    });

    return null;
  }

  onImageError(event: Event): void {
    const imgElement = event.target as HTMLImageElement;
    console.error('Image failed to load:', imgElement.src);
    imgElement.style.display = 'none';
  }
}
