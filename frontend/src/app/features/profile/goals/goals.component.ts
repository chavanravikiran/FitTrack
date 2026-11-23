import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Goal, GoalStatus } from '../../../core/models/goal.models';
import { GoalService } from '../../../core/services/goal.service';
import { NotifyService } from '../../../core/services/notify.service';
import { GOAL_TYPE_OPTIONS } from '../../../shared/constants/options';
import { GoalDialogComponent, GoalDialogData } from './goal-dialog.component';

@Component({
  selector: 'ft-goals',
  standalone: false,
  template: `
    <mat-card appearance="outlined" class="ft-card">
      <div class="head">
        <div>
          <h2 class="ft-section-title">Fitness goals</h2>
          <p class="ft-muted sub">Set realistic targets. You can change or retire a goal at any time.</p>
        </div>
        <button mat-flat-button type="button" (click)="open()"><mat-icon>add</mat-icon> New goal</button>
      </div>

      @if (loaded && !goals.length) {
        <ft-empty-state icon="flag" title="No goals yet" message="Add a goal for weight, steps, water, sleep or weekly workouts." />
      }

      @for (group of groups; track group.status) {
        @if (byStatus(group.status).length) {
          <h3 class="group">{{ group.label }}</h3>
          <div class="goal-list">
            @for (g of byStatus(group.status); track g.id) {
              <div class="goal" [class.goal--inactive]="g.status !== 'ACTIVE'">
                <mat-icon class="goal__icon" aria-hidden="true">{{ icon(g) }}</mat-icon>
                <div class="goal__body">
                  <strong>{{ g.title }}</strong>
                  <span class="ft-muted">
                    Target {{ g.targetValue }} {{ g.unit }}
                    @if (g.startValue !== null) { · started at {{ g.startValue }} {{ g.unit }} }
                    @if (g.targetDate) { · by {{ g.targetDate | date: 'mediumDate' }} }
                    @if (g.achievedAt) { · achieved {{ g.achievedAt | date: 'mediumDate' }} }
                  </span>
                </div>
                <button mat-icon-button [matMenuTriggerFor]="menu" [attr.aria-label]="'Actions for ' + g.title"><mat-icon>more_vert</mat-icon></button>
                <mat-menu #menu="matMenu" xPosition="before">
                  @if (g.status === 'ACTIVE') {
                    <button mat-menu-item (click)="open(g)"><mat-icon>edit</mat-icon>Edit</button>
                    <button mat-menu-item (click)="setStatus(g, 'ACHIEVED')"><mat-icon>emoji_events</mat-icon>Mark achieved</button>
                    <button mat-menu-item (click)="setStatus(g, 'ABANDONED')"><mat-icon>archive</mat-icon>Retire</button>
                  } @else {
                    <button mat-menu-item (click)="setStatus(g, 'ACTIVE')"><mat-icon>replay</mat-icon>Make active again</button>
                  }
                  <button mat-menu-item (click)="remove(g)"><mat-icon>delete</mat-icon>Delete</button>
                </mat-menu>
              </div>
            }
          </div>
        }
      }
    </mat-card>
  `,
  styles: `
    .head { display: flex; flex-wrap: wrap; gap: 12px; justify-content: space-between; align-items: flex-start; }
    .sub { margin: -6px 0 8px; font: var(--mat-sys-body-small); }
    .group { font: var(--mat-sys-label-large); color: var(--mat-sys-on-surface-variant); margin: 20px 0 8px; text-transform: uppercase; letter-spacing: .04em; }
    .goal-list { display: flex; flex-direction: column; gap: 8px; }
    .goal { display: grid; grid-template-columns: 40px 1fr auto; gap: 12px; align-items: center; padding: 12px; border-radius: 16px;
            background: var(--mat-sys-surface-container-low); border: 1px solid var(--mat-sys-outline-variant); }
    .goal--inactive { opacity: .75; }
    .goal__icon { width: 40px; height: 40px; display: grid; place-items: center; border-radius: 12px; font-size: 22px;
                  background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); }
    .goal__body { display: flex; flex-direction: column; min-width: 0; }
    .goal__body span { font: var(--mat-sys-body-small); }
  `,
})
export class GoalsComponent implements OnInit {
  goals: Goal[] = [];
  loaded = false;

  readonly groups: { status: GoalStatus; label: string }[] = [
    { status: 'ACTIVE', label: 'Active' },
    { status: 'ACHIEVED', label: 'Achieved' },
    { status: 'ABANDONED', label: 'Retired' },
  ];

  constructor(
    private readonly goalService: GoalService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  byStatus(status: GoalStatus): Goal[] {
    return this.goals.filter((g) => g.status === status);
  }

  icon(g: Goal): string {
    return GOAL_TYPE_OPTIONS.find((o) => o.value === g.goalType)?.icon ?? 'flag';
  }

  open(goal?: Goal): void {
    this.dialog
      .open<GoalDialogComponent, GoalDialogData, Goal>(GoalDialogComponent, { data: { goal }, width: '480px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((saved) => {
        if (saved) {
          this.notify.success(goal ? 'Goal updated' : 'Goal created');
          this.load();
        }
      });
  }

  setStatus(goal: Goal, status: GoalStatus): void {
    this.goalService.setStatus(goal.id, status).subscribe(() => {
      this.notify.success(status === 'ACHIEVED' ? 'Congratulations on reaching your goal!' : 'Goal updated');
      this.load();
    });
  }

  remove(goal: Goal): void {
    if (!confirm(`Delete "${goal.title}"? This cannot be undone.`)) return;
    this.goalService.delete(goal.id).subscribe(() => {
      this.notify.success('Goal deleted');
      this.load();
    });
  }

  private load(): void {
    this.goalService.list().subscribe((goals) => {
      this.goals = goals;
      this.loaded = true;
    });
  }
}
