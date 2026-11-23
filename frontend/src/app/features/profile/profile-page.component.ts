import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

const TABS = ['details', 'assessment', 'goals', 'security'] as const;
type Tab = (typeof TABS)[number];

@Component({
  selector: 'ft-profile-page',
  standalone: false,
  template: `
    <ft-page-header title="My Profile" icon="person" subtitle="Manage your details, fitness assessment, goals and account security." />
    <mat-tab-group [selectedIndex]="index" (selectedIndexChange)="select($event)" mat-stretch-tabs="false" animationDuration="150ms">
      <mat-tab label="Details"><ng-template matTabContent><ft-profile-details /></ng-template></mat-tab>
      <mat-tab label="Fitness assessment"><ng-template matTabContent><ft-assessment-edit /></ng-template></mat-tab>
      <mat-tab label="Goals"><ng-template matTabContent><ft-goals /></ng-template></mat-tab>
      <mat-tab label="Security"><ng-template matTabContent><ft-security /></ng-template></mat-tab>
    </mat-tab-group>
  `,
  styles: `mat-tab-group { margin-top: 4px; } ::ng-deep .mat-mdc-tab-body-content { padding-top: 20px; }`,
})
export class ProfilePageComponent {
  index = 0;

  /** Deep-linkable tab via ?tab=goals (bound by the router). */
  @Input() set tab(value: string | undefined) {
    const i = TABS.indexOf((value ?? 'details') as Tab);
    this.index = i >= 0 ? i : 0;
  }

  constructor(private readonly router: Router) {}

  select(i: number): void {
    this.index = i;
    this.router.navigate([], { queryParams: { tab: TABS[i] }, replaceUrl: true });
  }
}
