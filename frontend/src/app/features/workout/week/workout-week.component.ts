import { Component, EventEmitter, OnInit, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { CalendarEntry } from '../../../core/models/workout.models';
import { NotifyService } from '../../../core/services/notify.service';
import { WorkoutService } from '../../../core/services/workout.service';
import { DAY_STATUS } from '../../../shared/constants/labels';
import { toIsoDate } from '../../../shared/forms/validators';
import { RescheduleDialogComponent, RescheduleDialogData } from '../dialogs/reschedule-dialog.component';

interface WeekGroup {
  label: string;
  entries: CalendarEntry[];
}

@Component({
  selector: 'ft-workout-week',
  standalone: false,
  template: `
    <div class="toolbar">
      <button mat-icon-button (click)="shift(-7)" aria-label="Previous week"><mat-icon>chevron_left</mat-icon></button>
      <span class="range">{{ from | date: 'd MMM' }} – {{ to | date: 'd MMM yyyy' }}</span>
      <button mat-icon-button (click)="shift(7)" aria-label="Next week"><mat-icon>chevron_right</mat-icon></button>
      <button mat-button (click)="reset()">This week</button>
    </div>
    @if (!weeks.length) { <mat-progress-bar mode="indeterminate" /> }
    @for (w of weeks; track w.label) {
      <h3 class="week">{{ w.label }}</h3>
      <div class="grid">
        @for (e of w.entries; track e.date + '-' + e.planDayId) {
          <div class="day" [class.day--today]="e.date === today" [class.day--rest]="e.restDay" [class]="'tone-' + status[e.status].tone">
            <div class="day__top">
              <span class="dow">{{ e.date | date: 'EEE' }}</span>
              <span class="date">{{ e.date | date: 'd MMM' }}</span>
            </div>
            <strong class="title">{{ e.title }}</strong>
            <span class="badge"><mat-icon aria-hidden="true">{{ status[e.status].icon }}</mat-icon>{{ status[e.status].label }}</span>
            @if (e.rescheduledTo) { <span class="ft-muted small">→ {{ e.rescheduledTo | date: 'EEE d MMM' }}</span> }
            @if (e.rescheduledHere) { <span class="ft-muted small">Moved here</span> }
            @if (!e.restDay) {
              <div class="day__actions">
                <button mat-button (click)="open(e)">{{ e.status === 'COMPLETED' ? 'View' : 'Open' }}</button>
                @if ((e.status === 'SCHEDULED' || e.status === 'MISSED') && e.date >= minMove) {
                  <button mat-icon-button [matMenuTriggerFor]="menu" [attr.aria-label]="'More actions for ' + e.title"><mat-icon>more_vert</mat-icon></button>
                  <mat-menu #menu="matMenu">
                    <button mat-menu-item (click)="move(e)"><mat-icon>event_repeat</mat-icon>Move</button>
                    <button mat-menu-item (click)="skip(e)"><mat-icon>do_not_disturb_on</mat-icon>Skip</button>
                  </mat-menu>
                }
              </div>
            }
          </div>
        }
      </div>
    }
  `,
  styles: `
    .toolbar { display: flex; align-items: center; gap: 4px; margin-bottom: 8px; }
    .range { font: var(--mat-sys-title-medium); min-width: 170px; text-align: center; }
    .week { font: var(--mat-sys-label-large); color: var(--mat-sys-on-surface-variant); text-transform: uppercase; letter-spacing: .04em; margin: 16px 0 8px; }
    .grid { display: grid; gap: 10px; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); }
    .day { display: flex; flex-direction: column; gap: 4px; padding: 12px; border-radius: 16px; min-height: 130px;
           background: var(--mat-sys-surface-container-low); border: 1px solid var(--mat-sys-outline-variant); }
    .day--today { border: 2px solid var(--mat-sys-primary); }
    .day--rest { opacity: .7; }
    .day__top { display: flex; justify-content: space-between; font: var(--mat-sys-label-medium); color: var(--mat-sys-on-surface-variant); }
    .dow { font-weight: 700; color: var(--mat-sys-on-surface); }
    .title { font: var(--mat-sys-title-small); }
    .badge { display: inline-flex; align-items: center; gap: 4px; font: var(--mat-sys-label-medium); }
    .badge mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .tone-primary .badge { color: var(--mat-sys-primary); }
    .tone-error .badge { color: var(--mat-sys-error); }
    .tone-tertiary .badge { color: var(--mat-sys-tertiary); }
    .small { font: var(--mat-sys-body-small); }
    .day__actions { margin-top: auto; display: flex; justify-content: space-between; align-items: center; }
  `,
})
export class WorkoutWeekComponent implements OnInit {
  @Output() changed = new EventEmitter<void>();
  readonly status = DAY_STATUS;
  readonly today = toIsoDate(new Date())!;
  readonly minMove = this.today;
  from = this.monday(new Date());
  to = this.addDays(this.from, 13);
  weeks: WeekGroup[] = [];

  constructor(
    private readonly workouts: WorkoutService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
    private readonly router: Router,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  shift(days: number): void {
    this.from = this.addDays(this.from, days);
    this.to = this.addDays(this.to, days);
    this.load();
  }

  reset(): void {
    this.from = this.monday(new Date());
    this.to = this.addDays(this.from, 13);
    this.load();
  }

  open(e: CalendarEntry): void {
    this.router.navigate(['/workout'], { queryParams: { tab: 'today', date: e.date, planDayId: e.planDayId } });
  }

  move(e: CalendarEntry): void {
    this.dialog.open<RescheduleDialogComponent, RescheduleDialogData, boolean>(RescheduleDialogComponent, {
      data: { date: e.date, planDayId: e.planDayId, title: e.title }, width: '400px', maxWidth: '95vw',
    }).afterClosed().subscribe((moved) => {
      if (moved) {
        this.load();
        this.changed.emit();
      }
    });
  }

  skip(e: CalendarEntry): void {
    const reason = prompt('Optional: why are you skipping?');
    if (reason === null) return;
    this.workouts.skip(e.date, e.planDayId, reason || null).subscribe(() => {
      this.notify.info('Workout skipped');
      this.load();
      this.changed.emit();
    });
  }

  private load(): void {
    const from = toIsoDate(this.from)!;
    const to = toIsoDate(this.to)!;
    this.workouts.calendar(from, to).subscribe((entries) => {
      const mid = toIsoDate(this.addDays(this.from, 7))!;
      this.weeks = [
        { label: 'Week of ' + this.from.toLocaleDateString(undefined, { day: 'numeric', month: 'short' }), entries: entries.filter((e) => e.date < mid) },
        { label: 'Following week', entries: entries.filter((e) => e.date >= mid) },
      ];
    });
  }

  private monday(d: Date): Date {
    const copy = new Date(d.getFullYear(), d.getMonth(), d.getDate());
    const diff = (copy.getDay() + 6) % 7;
    copy.setDate(copy.getDate() - diff);
    return copy;
  }

  private addDays(d: Date, n: number): Date {
    const copy = new Date(d);
    copy.setDate(copy.getDate() + n);
    return copy;
  }
}
