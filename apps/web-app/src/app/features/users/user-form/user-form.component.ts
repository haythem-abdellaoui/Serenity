import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { UserService } from '../../../core/services/user.service';

@Component({
  selector: 'app-user-form',
  templateUrl: './user-form.component.html',
  styleUrls: ['./user-form.component.scss']
})
export class UserFormComponent implements OnInit {
  userForm!: FormGroup;
  userId!: number;
  loading = true;
  saving = false;
  errorMessage = '';
  availableRoles = ['PATIENT', 'DOCTOR', 'PHARMACIST', 'MARKETPLACE_MANAGER', 'ADMIN'];

  constructor(
    private readonly fb: FormBuilder,
    private readonly userService: UserService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.userId = +this.route.snapshot.params['id'];

    this.userForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: [{ value: '', disabled: true }],
      phone: [''],
      dateOfBirth: [''],
      password: ['', [Validators.minLength(8)]],
      role: ['']
    });

    this.loadUser();
  }

  loadUser(): void {
    this.userService.getUserById(this.userId).subscribe({
      next: (user) => {
        this.userForm.patchValue({
          firstName: user.firstName,
          lastName: user.lastName,
          email: user.email,
          phone: user.phone,
          dateOfBirth: user.dateOfBirth ? new Date(user.dateOfBirth).toISOString().split('T')[0] : '',
          role: user.role
        });
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load user';
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid) return;

    this.saving = true;
    this.errorMessage = '';

    const formValue = this.userForm.getRawValue();
    if (!formValue.password) {
      formValue.password = null;
    }

    this.userService.updateUser(this.userId, formValue).subscribe({
      next: () => {
        this.router.navigate(['/users']);
      },
      error: (err) => {
        this.saving = false;
        this.errorMessage = err.error?.message || 'Failed to update user';
      }
    });
  }
}
