import { Component, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from 'src/app/core/services/auth.service';

@Component({
  selector: 'app-doctor',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './doctor.component.html',
  styleUrl: './doctor.component.scss'
})
export class DoctorComponent {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  doctorForm!: FormGroup;
  selectedImage: string | null = null;
  imageFile: File | null = null;
  successMessage: string = '';
  errorMessage: string = '';
  userId!: number;

  constructor(private formBuilder: FormBuilder,private router: Router,private AuthService: AuthService) {
    this.initializeForm();
  }

  ngOnInit() {
    const storedId = localStorage.getItem('userId'); // assign to property
    if (!storedId) {
      this.router.navigate(['/auth/login']);
    }
    /*const user = this.AuthService.getCurrentUser();
    console.log('Current user:', user);
    if (!user || user.role !== 'DOCTOR') {
      this.router.navigate(['/auth/login']);
      return;
    }*/
    this.userId = Number(storedId);
  }

  initializeForm() {
    this.doctorForm = this.formBuilder.group({
      speciality: [
        '',
        [
          Validators.required,
          Validators.minLength(2),
          Validators.pattern('^[A-Za-z ]+$') 
        ]
    ],
      faceImage: ['', Validators.required]
    });
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      
      // Validate file type
      if (!file.type.startsWith('image/')) {
        this.errorMessage = 'Please select a valid image file';
        return;
      }

      // Validate file size (max 5MB)
      if (file.size > 5 * 1024 * 1024) {
        this.errorMessage = 'Image size must be less than 5MB';
        return;
      }

      this.imageFile = file;
      const reader = new FileReader();
      reader.onload = (e) => {
        this.selectedImage = e.target?.result as string;
        this.doctorForm.get('faceImage')?.setValue(file.name);
      };
      reader.readAsDataURL(file);
      this.errorMessage = '';
    }
  }

  triggerFileInput() {
    this.fileInput?.nativeElement?.click();
  }

  onSubmit() {
  if (this.doctorForm.valid && this.imageFile) {
    const speciality = this.doctorForm.get('speciality')?.value

    const token = this.AuthService.getToken();
    console.log('JWT fel page mte3 doctor:', token);

    this.AuthService.addDoctor(this.userId, speciality, this.imageFile).subscribe({
      next: () => {
        this.successMessage = 'Profile updated successfully!'
        const incrementedId = this.userId + 1;
        localStorage.setItem('userId', incrementedId.toString());
        this.userId = incrementedId;
        setTimeout(() => {
          this.router.navigate(['/auth/doctor-verification'])
        }, 3000)
      },
      error: () => {
        this.errorMessage = 'Error while creating doctor profile'
      }
    })
  } else {
    this.errorMessage = 'Please fill in all required fields'
  }
}

  removeImage() {
    this.selectedImage = null;
    this.imageFile = null;
    this.doctorForm.get('faceImage')?.reset();
    if (this.fileInput) {
      this.fileInput.nativeElement.value = '';
    }
  }
}
