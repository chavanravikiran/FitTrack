import { BreakpointObserver } from '@angular/cdk/layout';
import { Component, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatSidenav } from '@angular/material/sidenav';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { NavigationEnd, Router } from '@angular/router';
import { distinctUntilChanged, filter, map, switchMap, of } from 'rxjs';
import { ThemePreference } from '../../core/models/profile.models';
import { AuthService } from '../../core/services/auth.service';
import { ProfileService } from '../../core/services/profile.service';
import { ThemeService } from '../../core/services/theme.service';
import { BillingService } from '../../core/services/billing.service';
import { NotifyService } from '../../core/services/notify.service';
import { ADMIN_NAV_ITEM, NAV_ITEMS } from '../nav-items';

@Component({
  selector: 'ft-shell',
  standalone: false,
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent implements OnInit {
  @ViewChild(MatSidenav) sidenav?: MatSidenav;

  private readonly destroyRef = inject(DestroyRef);
  private photoObjectUrl: string | null = null;

  navItems = NAV_ITEMS;
  readonly primaryItems = NAV_ITEMS.filter((n) => n.primary);
  isHandset = false;
  photoUrl: SafeUrl | null = null;
  resending = false;
  verifyBannerDismissed = false;

  constructor(
    private readonly breakpoints: BreakpointObserver,
    private readonly router: Router,
    private readonly sanitizer: DomSanitizer,
    readonly auth: AuthService,
    readonly profileService: ProfileService,
    readonly theme: ThemeService,
    readonly billing: BillingService,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    if (this.auth.isAdmin()) {
      this.navItems = [...NAV_ITEMS, ADMIN_NAV_ITEM];
    }
    this.billing.loadSubscription().subscribe({ error: () => undefined });

    this.breakpoints
      .observe('(max-width: 959.98px)')
      .pipe(map((r) => r.matches), takeUntilDestroyed(this.destroyRef))
      .subscribe((handset) => (this.isHandset = handset));

    // Close the drawer after navigating on small screens.
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.isHandset) this.sidenav?.close();
      });

    this.profileService.load().subscribe((p) => {
      if (p.theme !== this.theme.theme) this.theme.setTheme(p.theme);
    });

    // Fetch the avatar whenever the profile says a photo exists.
    this.profileService.profile$
      .pipe(
        map((p) => (p?.hasPhoto ? `${p.userId}-${this.profileService.photoVersion}` : null)),
        distinctUntilChanged(),
        switchMap((key) => (key ? this.profileService.photoBlob() : of(null))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({ next: (blob) => this.setPhoto(blob), error: () => this.setPhoto(null) });

    this.destroyRef.onDestroy(() => this.setPhoto(null));
  }

  initials(name: string | null | undefined): string {
    if (!name) return '?';
    return name
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((p) => p[0]!.toUpperCase())
      .join('');
  }

  setTheme(t: ThemePreference): void {
    this.theme.setTheme(t);
    this.profileService.updateTheme(t).subscribe();
  }

  logout(): void {
    this.profileService.clear();
    this.billing.clear();
    this.auth.logout();
  }

  resendVerification(): void {
    this.resending = true;
    this.auth.resendVerification().subscribe({
      next: (msg) => {
        this.resending = false;
        this.notify.success(msg || 'Verification email sent');
      },
      error: () => (this.resending = false),
    });
  }

  private setPhoto(blob: Blob | null): void {
    if (this.photoObjectUrl) {
      URL.revokeObjectURL(this.photoObjectUrl);
      this.photoObjectUrl = null;
    }
    if (blob) {
      this.photoObjectUrl = URL.createObjectURL(blob);
      this.photoUrl = this.sanitizer.bypassSecurityTrustUrl(this.photoObjectUrl);
    } else {
      this.photoUrl = null;
    }
  }
}
