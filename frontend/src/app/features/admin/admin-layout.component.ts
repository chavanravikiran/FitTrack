import { Component } from '@angular/core';

@Component({
  selector: 'ft-admin-layout',
  standalone: false,
  template: `
    <ft-page-header title="Admin" icon="admin_panel_settings" subtitle="Run FitTrack: users, subscriptions, plans and revenue." />
    <nav mat-tab-nav-bar [tabPanel]="panel" mat-stretch-tabs="false" aria-label="Admin sections">
      @for (l of links; track l.path) {
        <a mat-tab-link [routerLink]="l.path" routerLinkActive #rla="routerLinkActive" [active]="rla.isActive">
          <mat-icon aria-hidden="true" class="tab-icon">{{ l.icon }}</mat-icon>{{ l.label }}
        </a>
      }
    </nav>
    <mat-tab-nav-panel #panel class="panel"><router-outlet /></mat-tab-nav-panel>
  `,
  styles: `.panel { display: block; padding-top: 20px; } .tab-icon { margin-right: 6px; font-size: 20px; width: 20px; height: 20px; }`,
})
export class AdminLayoutComponent {
  readonly links = [
    { path: 'overview', label: 'Overview', icon: 'monitoring' },
    { path: 'users', label: 'Users', icon: 'group' },
    { path: 'plans', label: 'Plans', icon: 'sell' },
    { path: 'payments', label: 'Payments', icon: 'receipt_long' },
    { path: 'videos', label: 'Videos', icon: 'video_library' },
  ];
}
