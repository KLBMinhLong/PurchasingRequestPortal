import { CommonModule } from '@angular/common';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { AuthService } from '../../../../core/services/auth.service';
import { User } from '../../../../core/models/user.model';
import { UserManagementService } from '../../services/user-management.service';

@Component({
  selector: 'app-user-management-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-management-page.component.html',
  styleUrl: './user-management-page.component.scss'
})
export class UserManagementPageComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly users = signal<User[]>([]);
  readonly loading = signal(false);
  readonly saving = signal(false);
  readonly deletingId = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);
  readonly editingUser = signal<User | null>(null);

  readonly hasUsers = computed(() => this.users().length > 0);

  readonly userForm = this.formBuilder.nonNullable.group({
    username: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.minLength(8)]],
    firstName: [''],
    lastName: [''],
    phone: [''],
    roleCodes: ['ADMIN']
  });

  constructor(
    private readonly formBuilder: FormBuilder,
    private readonly userManagementService: UserManagementService,
    private readonly authService: AuthService
  ) {
    this.loadUsers();
  }

  trackByUserId(_: number, user: User): string {
    return user.id;
  }

  startCreate(): void {
    this.editingUser.set(null);
    this.userForm.reset({
      username: '',
      email: '',
      password: '',
      firstName: '',
      lastName: '',
      phone: '',
      roleCodes: 'ADMIN'
    });
  }

  startEdit(user: User): void {
    this.editingUser.set(user);
    this.userForm.reset({
      username: user.username,
      email: user.email,
      password: '',
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      phone: user.phone ?? '',
      roleCodes: user.roleCodes.join(',') || 'ADMIN'
    });
  }

  submit(): void {
    if (this.userForm.invalid) {
      this.userForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    this.errorMessage.set(null);

    const formValue = this.userForm.getRawValue();
    const roleCodes = formValue.roleCodes
      .split(',')
      .map((role) => role.trim())
      .filter((role) => role.length > 0);

    const editing = this.editingUser();

    const request$ = editing
      ? this.userManagementService.updateUser(editing.id, {
          username: formValue.username,
          email: formValue.email,
          password: formValue.password || undefined,
          firstName: formValue.firstName || undefined,
          lastName: formValue.lastName || undefined,
          phone: formValue.phone || undefined,
          roleCodes
        })
      : this.userManagementService.createUser({
          username: formValue.username,
          email: formValue.email,
          password: formValue.password,
          firstName: formValue.firstName || undefined,
          lastName: formValue.lastName || undefined,
          phone: formValue.phone || undefined,
          roleCodes,
          active: true,
          locked: false,
          attributes: {}
        });

    request$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.loadUsers();
        this.startCreate();
      },
      error: () => {
        this.saving.set(false);
        this.errorMessage.set('Khong the luu nguoi dung. Vui long thu lai.');
      }
    });
  }

  deleteUser(user: User): void {
    this.deletingId.set(user.id);
    this.userManagementService
      .deleteUser(user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.loadUsers();
        },
        error: () => {
          this.deletingId.set(null);
          this.errorMessage.set('Khong the xoa nguoi dung.');
        }
      });
  }

  logout(): void {
    this.authService.logout().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      error: () => {
        // Local cleanup is still enforced by AuthService when backend logout fails.
      }
    });
  }

  private loadUsers(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    this.userManagementService
      .getUsers()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (users) => {
          this.users.set(users);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.errorMessage.set('Khong the tai danh sach nguoi dung tu Backend.');
        }
      });
  }
}
