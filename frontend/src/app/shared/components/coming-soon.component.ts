import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

/** Route placeholder for modules scheduled in later phases. Clearly labelled - no fake data. */
@Component({
  selector: 'ft-coming-soon',
  standalone: false,
  template: `
    <ft-page-header [title]="title" [icon]="icon" />
    <mat-card appearance="outlined">
      <ft-empty-state icon="construction" [title]="title + ' is on the way'"
        [message]="'This module is scheduled for implementation phase ' + phase + '. Your profile and fitness assessment are already saved and will power it.'">
        <a mat-flat-button routerLink="/dashboard">Back to dashboard</a>
      </ft-empty-state>
    </mat-card>
  `,
})
export class ComingSoonComponent {
  readonly title: string;
  readonly icon: string;
  readonly phase: number;

  constructor(route: ActivatedRoute) {
    const data = route.snapshot.data;
    this.title = data['title'] ?? 'Coming soon';
    this.icon = data['icon'] ?? 'schedule';
    this.phase = data['phase'] ?? 0;
  }
}
