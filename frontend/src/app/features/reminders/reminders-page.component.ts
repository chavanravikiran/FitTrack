import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { AppNotification, Reminder } from '../../core/models/reminder.models';
import { NotifyService } from '../../core/services/notify.service';
import { ReminderService } from '../../core/services/reminder.service';
import { DAY_OPTIONS, labelOf } from '../../shared/constants/options';
import { REMINDER_TYPES } from '../../shared/constants/labels';
import { ReminderDialogComponent } from './reminder-dialog.component';

@Component({
  selector: 'ft-reminders-page',
  standalone: false,
  template: `
    <ft-page-header title="Reminders" icon="notifications" subtitle="Gentle nudges for water, workouts, walks, meals, weigh-ins and sleep.">
      <button mat-flat-button (click)="edit()"><mat-icon>add</mat-icon> New reminder</button>
    </ft-page-header>

    @if (permission !== 'granted' && supported) {
      <div class="banner" role="status">
        <mat-icon aria-hidden="true">notifications_active</mat-icon>
        <span>Allow browser notifications to see reminders pop up while FitTrack is open.</span>
        <button mat-button (click)="askPermission()" [disabled]="permission === 'denied'">
          {{ permission === 'denied' ? 'Blocked in browser settings' : 'Allow' }}
        </button>
      </div>
    }

    <div class="layout">
      <div>
        @if (reminders === null) {
          <mat-progress-bar mode="indeterminate" />
        } @else if (!reminders.length) {
          <mat-card appearance="outlined" class="ft-card">
            <ft-empty-state icon="alarm_add" title="No reminders yet" message="Start with a water reminder every 2 hours or a workout reminder on your training days.">
              <div class="quick">
                @for (t of quick; track t.value) {
                  <button mat-stroked-button (click)="edit(undefined, t.value)"><mat-icon>{{ t.icon }}</mat-icon> {{ t.label }}</button>
                }
              </div>
            </ft-empty-state>
          </mat-card>
        } @else {
          <div class="list">
            @for (r of reminders; track r.id) {
              <mat-card appearance="outlined" class="rem" [class.rem--off]="!r.enabled">
                <mat-icon class="rem__icon" aria-hidden="true">{{ icon(r) }}</mat-icon>
                <div class="rem__body">
                  <strong>{{ r.title }}</strong>
                  <span class="time">{{ r.time }}@if (r.intervalMinutes) { – {{ r.endTime ?? '21:00' }}, every {{ r.intervalMinutes }} min }</span>
                  <span class="ft-muted small">{{ days(r) }} · {{ r.channel === 'EMAIL' ? 'Email + app' : 'App notification' }}</span>
                  @if (r.enabled && r.nextFireAt) { <span class="ft-muted small">Next: {{ r.nextFireAt | date: 'EEE d MMM, h:mm a' }}</span> }
                </div>
                <mat-slide-toggle [checked]="r.enabled" (change)="toggle(r, $event.checked)" [attr.aria-label]="'Enable ' + r.title" />
                <button mat-icon-button [matMenuTriggerFor]="menu" [attr.aria-label]="'Actions for ' + r.title"><mat-icon>more_vert</mat-icon></button>
                <mat-menu #menu="matMenu">
                  <button mat-menu-item (click)="edit(r)"><mat-icon>edit</mat-icon>Edit</button>
                  <button mat-menu-item (click)="remove(r)"><mat-icon>delete</mat-icon>Delete</button>
                </mat-menu>
              </mat-card>
            }
          </div>
        }
      </div>

      <mat-card appearance="outlined" class="ft-card">
        <div class="head">
          <h2 class="ft-section-title">Recent notifications</h2>
          @if (notifications.length) { <button mat-button (click)="readAll()">Mark all read</button> }
        </div>
        @if (notifications.length) {
          <ul class="notes">
            @for (n of notifications; track n.id) {
              <li [class.unread]="!n.read">
                <strong>{{ n.title }}</strong>
                @if (n.body) { <span>{{ n.body }}</span> }
                <span class="ft-muted small">{{ n.createdAt | date: 'EEE d MMM, h:mm a' }}</span>
              </li>
            }
          </ul>
        } @else {
          <p class="ft-muted">Reminders you receive will appear here.</p>
        }
      </mat-card>
    </div>
  `,
  styles: `
    .banner { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; padding: 10px 14px; border-radius: 14px; margin-bottom: 16px;
              background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
    .banner span { flex: 1; min-width: 200px; }
    .layout { display: grid; grid-template-columns: 1fr; gap: 16px; }
    @media (min-width: 1000px) { .layout { grid-template-columns: 3fr 2fr; align-items: start; } }
    .quick { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 8px; }
    .list { display: flex; flex-direction: column; gap: 10px; }
    .rem { display: flex; flex-direction: row; align-items: center; gap: 12px; padding: 14px 16px; border-radius: 18px; }
    .rem--off { opacity: .6; }
    .rem__icon { width: 44px; height: 44px; border-radius: 14px; display: grid; place-items: center; font-size: 24px;
                 background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); flex: none; }
    .rem__body { flex: 1; display: flex; flex-direction: column; min-width: 0; }
    .time { font: var(--mat-sys-title-medium); font-variant-numeric: tabular-nums; }
    .small { font: var(--mat-sys-body-small); }
    .head { display: flex; justify-content: space-between; align-items: center; }
    .head .ft-section-title { margin: 0; }
    .notes { list-style: none; padding: 0; margin: 8px 0 0; }
    .notes li { display: flex; flex-direction: column; gap: 2px; padding: 10px 0 10px 12px; border-bottom: 1px solid var(--mat-sys-outline-variant); border-left: 3px solid transparent; }
    .notes li.unread { border-left-color: var(--mat-sys-primary); }
  `,
})
export class RemindersPageComponent implements OnInit {
  reminders: Reminder[] | null = null;
  notifications: AppNotification[] = [];
  readonly quick = REMINDER_TYPES.filter((t) => ['WATER', 'WORKOUT', 'WALK', 'SLEEP'].includes(t.value));
  readonly supported = typeof window !== 'undefined' && 'Notification' in window;
  permission: NotificationPermission = this.supported ? Notification.permission : 'denied';

  constructor(
    private readonly reminderService: ReminderService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  icon(r: Reminder): string {
    return REMINDER_TYPES.find((t) => t.value === r.type)?.icon ?? 'notifications';
  }

  days(r: Reminder): string {
    if (r.days.length === 7) return 'Every day';
    if (r.days.length === 5 && !r.days.includes('SATURDAY') && !r.days.includes('SUNDAY')) return 'Weekdays';
    return r.days.map((d) => labelOf(DAY_OPTIONS, d)).join(', ');
  }

  edit(r?: Reminder, type?: string): void {
    this.dialog.open(ReminderDialogComponent, { data: { reminder: r, type }, width: '520px', maxWidth: '95vw' })
      .afterClosed().subscribe((saved) => saved && this.load());
  }

  toggle(r: Reminder, enabled: boolean): void {
    this.reminderService.toggle(r.id, enabled).subscribe((u) => Object.assign(r, u));
  }

  remove(r: Reminder): void {
    if (!confirm(`Delete "${r.title}"?`)) return;
    this.reminderService.delete(r.id).subscribe(() => {
      this.notify.success('Reminder deleted');
      this.load();
    });
  }

  readAll(): void {
    this.reminderService.markAllRead().subscribe(() => this.notifications.forEach((n) => (n.read = true)));
  }

  askPermission(): void {
    Notification.requestPermission().then((p) => {
      this.permission = p;
      if (p === 'granted') this.notify.success('Browser notifications enabled');
    });
  }

  private load(): void {
    this.reminderService.list().subscribe((r) => (this.reminders = r));
    this.reminderService.notifications(false, 0, 15).subscribe((p) => (this.notifications = p.content));
  }
}
