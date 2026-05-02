import { Component, OnInit, AfterViewInit, ViewChild, ElementRef, OnDestroy } from '@angular/core';
import { AuthService } from '../../core/services/auth.service';
import { UserService } from '../../core/services/user.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { PatientService } from '../../core/services/patient.service';
import { UserResponse } from '../../shared/models/user.model';
import { DashboardStats } from '../../models/dashboard.model';
import { Chart, ArcElement, Tooltip, Legend, PieController } from 'chart.js';

Chart.register(ArcElement, Tooltip, Legend, PieController);

@Component({
  selector: 'app-statistics',
  templateUrl: './statistics.component.html',
  styleUrls: ['./statistics.component.scss']
})
export class StatisticsComponent implements OnInit, AfterViewInit, OnDestroy {
  private user: UserResponse | null = null;
  stats: DashboardStats | null = null;
  loading = true;
  private chart: Chart | null = null;

  @ViewChild('severityChart') chartRef!: ElementRef<HTMLCanvasElement>;

  constructor(
    public readonly authService: AuthService,
    private readonly userService: UserService,
    private readonly dashboardService: DashboardService,
    private readonly patientService: PatientService
  ) {}

  ngOnInit(): void {
    this.userService.getCurrentUser().subscribe({
      next: (user) => this.user = user
    });

    this.dashboardService.getStats().subscribe({
      next: (stats) => {
        this.stats = stats;
        
        // Fetch patients to get the total count
        this.patientService.getAllPatients().subscribe({
          next: (patients) => {
            if (this.stats) {
              this.stats.totalPatients = patients ? patients.length : 0;
            }
            this.loading = false;
            setTimeout(() => this.buildChart(), 0);
          },
          error: () => {
            this.loading = false;
            setTimeout(() => this.buildChart(), 0);
          }
        });
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  ngAfterViewInit(): void {
    // Chart will be built after stats arrive
  }

  ngOnDestroy(): void {
    this.chart?.destroy();
  }

  getDisplayName(): string {
    if (this.user?.profile?.isAnonymous) return 'Anonymous';
    if (this.user?.firstName) return this.user.firstName;
    return (this.authService.getCurrentUser()?.email || '').split('@')[0];
  }

  private buildChart(): void {
    if (!this.stats || !this.chartRef) return;
    this.chart?.destroy();

    const ctx = this.chartRef.nativeElement.getContext('2d');
    if (!ctx) return;

    this.chart = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: ['Low', 'Medium', 'High'],
        datasets: [{
          data: [
            this.stats.severityLow,
            this.stats.severityMedium,
            this.stats.severityHigh
          ],
          backgroundColor: [
            '#3BA4A0',
            '#E8A951',
            '#D96B6B'
          ],
          borderColor: '#ffffff',
          borderWidth: 3,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              padding: 20,
              usePointStyle: true,
              pointStyleWidth: 12,
              font: {
                family: "'Nunito', sans-serif",
                size: 13,
                weight: 'bold'
              },
              color: '#2A4745'
            }
          },
          tooltip: {
            backgroundColor: '#2A4745',
            titleFont: { family: "'Nunito', sans-serif", weight: 'bold' },
            bodyFont: { family: "'Nunito', sans-serif" },
            cornerRadius: 10,
            padding: 12
          }
        }
      }
    });
  }
}
