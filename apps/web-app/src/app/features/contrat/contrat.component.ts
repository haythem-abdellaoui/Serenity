import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute } from '@angular/router';
import { DoctorVerificationService } from 'src/app/core/services/doctor-verification.service';


@Component({
  selector: 'app-contrat',
  templateUrl: './contrat.component.html',
  styleUrl: './contrat.component.scss'
})
export class ContratComponent implements OnInit {
  agreed = false;
  agreementTimestamp = '';
  token: string | null = null;

  constructor(private route: ActivatedRoute, 
    private http: HttpClient,
    private doctorVerificationService: DoctorVerificationService) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  handleAgree(): void {
    if (!this.token) return;

    this.doctorVerificationService.approveContract(this.token)
      .subscribe({
        next: () => {
          const now = new Date();
          this.agreementTimestamp = now.toLocaleString('en-US', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
          });
          this.agreed = true;
        },
        error: (err) => {
          console.error('Approval failed', err);
        }
      });
  }
}