import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { finalize } from 'rxjs';
import { Gender, Profile, ThemePreference } from '../../../core/models/profile.models';
import { AuthService } from '../../../core/services/auth.service';
import { NotifyService } from '../../../core/services/notify.service';
import { ProfileService } from '../../../core/services/profile.service';
import { ThemeService } from '../../../core/services/theme.service';
import { apiErrorMessage, applyServerErrors } from '../../../core/utils/api-error';
import { GENDER_OPTIONS } from '../../../shared/constants/options';
import { fromIsoDate, toIsoDate } from '../../../shared/forms/validators';

const MAX_PHOTO_BYTES = 5 * 1024 * 1024;

@Component({
  selector: 'ft-profile-details',
  standalone: false,
  templateUrl: './profile-details.component.html',
  styleUrl: './profile-details.component.scss',
})
export class ProfileDetailsComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private objectUrl: string | null = null;

  readonly genders = GENDER_OPTIONS;
  readonly maxDob = (() => { const d = new Date(); d.setFullYear(d.getFullYear() - 16); return d; })();
  readonly timeZones: string[] = (Intl as unknown as { supportedValuesOf?: (k: string) => string[] }).supportedValuesOf?.('timeZone') ?? ['UTC'];

  readonly form = this.fb.group({
    fullName: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]),
    dateOfBirth: this.fb.control<Date | null>(null, Validators.required),
    gender: this.fb.control<Gender | null>(null, Validators.required),
    heightCm: this.fb.control<number | null>(null, [Validators.required, Validators.min(90), Validators.max(250)]),
    timeZone: this.fb.nonNullable.control('UTC', Validators.required),
    theme: this.fb.nonNullable.control<ThemePreference>('SYSTEM'),
  });

  profile: Profile | null = null;
  photoUrl: SafeUrl | null = null;
  saving = false;
  uploading = false;
  error: string | null = null;

  constructor(
    private readonly profileService: ProfileService,
    private readonly auth: AuthService,
    private readonly theme: ThemeService,
    private readonly notify: NotifyService,
    private readonly sanitizer: DomSanitizer,
  ) {}

  ngOnInit(): void {
    this.profileService.load().subscribe((p) => this.setProfile(p));
    this.destroyRef.onDestroy(() => this.revoke());
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.saving = true;
    this.error = null;
    this.profileService
      .update({
        fullName: v.fullName.trim(),
        dateOfBirth: toIsoDate(v.dateOfBirth)!,
        gender: v.gender!,
        heightCm: Number(v.heightCm),
        timeZone: v.timeZone,
        theme: v.theme,
      })
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (p) => {
          this.setProfile(p, false);
          this.theme.setTheme(p.theme);
          this.auth.patchUser({ fullName: p.fullName });
          this.form.markAsPristine();
          this.notify.success('Profile saved');
        },
        error: (err) => {
          applyServerErrors(this.form, err);
          this.error = apiErrorMessage(err);
        },
      });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    input.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type)) {
      this.notify.error('Please choose a JPEG or PNG image.');
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      this.notify.error('Image is too large (maximum 5 MB).');
      return;
    }
    this.uploading = true;
    this.profileService
      .uploadPhoto(file)
      .pipe(finalize(() => (this.uploading = false)))
      .subscribe((p) => {
        this.profile = p;
        this.loadPhoto();
        this.notify.success('Photo updated');
      });
  }

  removePhoto(): void {
    this.profileService.deletePhoto().subscribe(() => {
      if (this.profile) this.profile = { ...this.profile, hasPhoto: false };
      this.revoke();
      this.photoUrl = null;
      this.notify.success('Photo removed');
    });
  }

  private setProfile(p: Profile, patchForm = true): void {
    this.profile = p;
    if (patchForm) {
      this.form.reset({
        fullName: p.fullName,
        dateOfBirth: fromIsoDate(p.dateOfBirth),
        gender: p.gender,
        heightCm: p.heightCm,
        timeZone: p.timeZone,
        theme: p.theme,
      });
    }
    if (p.hasPhoto && !this.photoUrl) this.loadPhoto();
  }

  private loadPhoto(): void {
    this.profileService.photoBlob().subscribe({
      next: (blob) => {
        this.revoke();
        this.objectUrl = URL.createObjectURL(blob);
        this.photoUrl = this.sanitizer.bypassSecurityTrustUrl(this.objectUrl);
      },
      error: () => (this.photoUrl = null),
    });
  }

  private revoke(): void {
    if (this.objectUrl) {
      URL.revokeObjectURL(this.objectUrl);
      this.objectUrl = null;
    }
  }
}
