import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of, switchMap, timer } from 'rxjs';
import { AppNotification } from '../core/models/reminder.models';
import { ReminderService } from '../core/services/reminder.service';

/**
 * Toolbar bell. Polls the unread count every minute (silently) and, when new reminders arrive while the
 * app is open, shows a browser notification if the user allowed it.
 */
@Component({
  selector: 'ft-notification-bell',
  standalone: false,
  template: `
    <button mat-icon-button [matMenuTriggerFor]="menu" (menuOpened)="openMenu()" aria-label="Notifications" matTooltip="Notifications">
      <mat-icon [matBadge]="unread || null" matBadgeColor="warn" matBadgeSize="small" aria-hidden="false">notifications</mat-icon>
    </button>
    <mat-menu #menu="matMenu" xPosition="before" class="bell-menu">
      <div class="menu" (click)="$event.stopPropagation()" (keydown)="$event.stopPropagation()">
        <div class="menu__head">
          <strong>Notifications</strong>
          @if (items.length) { <button mat-button (click)="readAll()">Mark all read</button> }
        </div>
        @for (n of items; track n.id) {
          <div class="item">
            <strong>{{ n.title }}</strong>
            @if (n.body) { <span>{{ n.body }}</span> }
            <span class="ft-muted small">{{ n.createdAt | date: 'shortTime' }}</span>
          </div>
        } @empty {
          <p class="ft-muted empty">You're all caught up.</p>
        }
        <a mat-button routerLink="/reminders" class="all">Manage reminders</a>
      </div>
    </mat-menu>
  `,
  styles: `
    .menu { width: 300px; max-width: 85vw; padding: 4px 12px; }
    .menu__head { display: flex; justify-content: space-between; align-items: center; }
    .item { display: flex; flex-direction: column; padding: 8px 0; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .small { font: var(--mat-sys-body-small); }
    .empty { padding: 12px 0; }
    .all { width: 100%; margin-top: 4px; }
  `,
})
export class NotificationBellComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  unread = 0;
  items: AppNotification[] = [];
  private lastSeen = -1;

  constructor(private readonly reminders: ReminderService) {}

  ngOnInit(): void {
    this.reminders.unread$.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((n) => (this.unread = n));
    timer(0, 60_000)
      .pipe(
        switchMap(() => this.reminders.pollUnread().pipe(catchError(() => of(this.unread)))),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((n) => {
        if (this.lastSeen >= 0 && n > this.lastSeen) this.showBrowserNotification();
        this.lastSeen = n;
      });
  }

  openMenu(): void {
    this.reminders.recentUnread().subscribe((items) => (this.items = items));
  }

  readAll(): void {
    this.reminders.markAllRead().subscribe(() => {
      this.items = [];
      this.lastSeen = 0;
    });
  }

  private showBrowserNotification(): void {
    if (!('Notification' in window) || Notification.permission !== 'granted') return;
    this.reminders.recentUnread().subscribe((items) => {
      const latest = items[0];
      if (latest) {
        new Notification(latest.title, { body: latest.body ?? '', icon: 'favicon.ico', tag: `ft-${latest.id}` });
      }
    });
  }
}
