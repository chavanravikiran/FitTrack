import { Component, OnInit } from '@angular/core';
import { PageEvent } from '@angular/material/paginator';
import { HistoryItem, PersonalRecord } from '../../../core/models/workout.models';
import { WorkoutService } from '../../../core/services/workout.service';

@Component({
  selector: 'ft-workout-history',
  standalone: false,
  template: `
    <div class="layout">
      <mat-card appearance="outlined" class="ft-card">
        <h2 class="ft-section-title">Workout history</h2>
        @if (items.length) {
          <ul class="list">
            @for (h of items; track h.sessionId) {
              <li>
                <mat-icon [class]="'st st--' + h.status.toLowerCase()" aria-hidden="true">
                  {{ h.status === 'COMPLETED' ? 'check_circle' : h.status === 'SKIPPED' ? 'do_not_disturb_on' : 'event_repeat' }}
                </mat-icon>
                <div class="body">
                  <strong>{{ h.title }}</strong>
                  <span class="ft-muted small">
                    {{ h.date | date: 'EEE, d MMM yyyy' }} · {{ h.status | titlecase }}
                    @if (h.status === 'COMPLETED') { · {{ h.setsCompleted }} sets
                      @if (h.perceivedEffort) { · effort {{ h.perceivedEffort }}/10 }
                      @if (h.caloriesEst) { · ~{{ h.caloriesEst }} kcal }
                    }
                  </span>
                  @if (h.notes) { <span class="ft-muted small note">"{{ h.notes }}"</span> }
                </div>
              </li>
            }
          </ul>
          <mat-paginator [length]="total" [pageIndex]="page" [pageSize]="20" [hidePageSize]="true" (page)="onPage($event)" aria-label="Select history page" />
        } @else if (loaded) {
          <ft-empty-state icon="history" title="No workouts yet" message="Complete your first workout and it will show up here." />
        }
      </mat-card>

      <mat-card appearance="outlined" class="ft-card">
        <h2 class="ft-section-title"><mat-icon inline aria-hidden="true">emoji_events</mat-icon> Personal records</h2>
        @if (records.length) {
          <ul class="list">
            @for (r of records; track r.exerciseId + r.type) {
              <li>
                <div class="body">
                  <strong>{{ r.exerciseName }}</strong>
                  <span class="ft-muted small">{{ label(r) }} · {{ r.achievedOn | date: 'd MMM yyyy' }}</span>
                </div>
              </li>
            }
          </ul>
        } @else {
          <p class="ft-muted">Log reps, weights or hold times to start setting records.</p>
        }
      </mat-card>
    </div>
  `,
  styles: `
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; }
    @media (min-width: 1000px) { .layout { grid-template-columns: 3fr 2fr; align-items: start; } }
    .list { list-style: none; padding: 0; margin: 0; }
    .list li { display: flex; gap: 12px; align-items: flex-start; padding: 10px 0; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .list li:last-child { border-bottom: 0; }
    .body { display: flex; flex-direction: column; gap: 2px; }
    .small { font: var(--mat-sys-body-small); }
    .note { font-style: italic; }
    .st--completed { color: var(--mat-sys-primary); }
    .st--skipped, .st--rescheduled { color: var(--mat-sys-on-surface-variant); }
  `,
})
export class WorkoutHistoryComponent implements OnInit {
  items: HistoryItem[] = [];
  records: PersonalRecord[] = [];
  total = 0;
  page = 0;
  loaded = false;

  constructor(private readonly workouts: WorkoutService) {}

  ngOnInit(): void {
    this.load();
    this.workouts.personalRecords().subscribe((r) => (this.records = r));
  }

  onPage(e: PageEvent): void {
    this.page = e.pageIndex;
    this.load();
  }

  label(r: PersonalRecord): string {
    switch (r.type) {
      case 'MAX_WEIGHT': return `Heaviest: ${r.value} kg`;
      case 'MAX_REPS': return `Most reps: ${r.value}`;
      default: return `Longest hold: ${r.value}s`;
    }
  }

  private load(): void {
    this.workouts.history(this.page, 20).subscribe((p) => {
      this.items = p.content;
      this.total = p.totalElements;
      this.loaded = true;
    });
  }
}
