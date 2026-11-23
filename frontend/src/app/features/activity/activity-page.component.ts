import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';

const TABS = ['steps', 'water', 'sleep'] as const;

@Component({
  selector: 'ft-activity-page',
  standalone: false,
  template: `
    <ft-page-header title="Activity" icon="directions_walk" subtitle="Track steps, water and sleep - small daily habits add up." />
    <mat-tab-group [selectedIndex]="index" (selectedIndexChange)="select($event)" mat-stretch-tabs="false" animationDuration="150ms">
      <mat-tab><ng-template mat-tab-label><mat-icon class="ti">directions_walk</mat-icon>Steps</ng-template><ng-template matTabContent><ft-steps /></ng-template></mat-tab>
      <mat-tab><ng-template mat-tab-label><mat-icon class="ti">water_drop</mat-icon>Water</ng-template><ng-template matTabContent><ft-water /></ng-template></mat-tab>
      <mat-tab><ng-template mat-tab-label><mat-icon class="ti">bedtime</mat-icon>Sleep</ng-template><ng-template matTabContent><ft-sleep /></ng-template></mat-tab>
    </mat-tab-group>
  `,
  styles: `.ti { margin-right: 6px; } ::ng-deep .mat-mdc-tab-body-content { padding-top: 20px; }`,
})
export class ActivityPageComponent {
  index = 0;

  @Input() set tab(value: string | undefined) {
    const i = TABS.indexOf((value ?? 'steps') as (typeof TABS)[number]);
    this.index = i >= 0 ? i : 0;
  }

  constructor(private readonly router: Router) {}

  select(i: number): void {
    this.index = i;
    this.router.navigate([], { queryParams: { tab: TABS[i] }, replaceUrl: true });
  }
}
