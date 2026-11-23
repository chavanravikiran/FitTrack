import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs';
import { DayWorkout, ExerciseSummary, SessionExercise, SetLogRequest } from '../../../core/models/workout.models';
import { NotifyService } from '../../../core/services/notify.service';
import { WorkoutService } from '../../../core/services/workout.service';
import { apiErrorMessage } from '../../../core/utils/api-error';
import { ExerciseDetailDialogComponent } from '../../../shared/components/exercise-detail-dialog.component';
import { DAY_STATUS } from '../../../shared/constants/labels';
import { toIsoDate } from '../../../shared/forms/validators';
import { AlternativesDialogComponent, AlternativesDialogData } from '../dialogs/alternatives-dialog.component';
import { CompleteDialogComponent, CompleteResult } from '../dialogs/complete-dialog.component';
import { RescheduleDialogComponent, RescheduleDialogData } from '../dialogs/reschedule-dialog.component';

interface SetRow {
  setNumber: number;
  reps: number | null;
  weight: number | null;
  seconds: number | null;
  done: boolean;
}

interface ExerciseCard {
  source: SessionExercise;
  exercise: ExerciseSummary;
  timed: boolean;
  rows: SetRow[];
  dirty: boolean;
  saving: boolean;
  /** Set when the user swapped in an alternative for this session. */
  substituteFor?: string;
}

@Component({
  selector: 'ft-workout-today',
  standalone: false,
  templateUrl: './workout-today.component.html',
  styleUrl: './workout-today.component.scss',
})
export class WorkoutTodayComponent implements OnChanges {
  @Input() date?: string;
  @Input() planDayId?: string;
  @Output() changed = new EventEmitter<void>();

  day: DayWorkout | null = null;
  cards: ExerciseCard[] = [];
  loading = true;
  busy = false;
  error: string | null = null;
  readonly status = DAY_STATUS;
  readonly today = toIsoDate(new Date())!;

  constructor(
    private readonly workouts: WorkoutService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
  ) {}

  ngOnChanges(): void {
    this.load();
  }

  get canStart(): boolean {
    const d = this.day;
    return !!d && !d.restDay && (d.status === 'SCHEDULED' || d.status === 'MISSED' || d.status === 'SKIPPED') && d.date <= this.today;
  }

  get inProgress(): boolean {
    return this.day?.status === 'IN_PROGRESS';
  }

  get editable(): boolean {
    return this.day?.status === 'IN_PROGRESS' || this.day?.status === 'COMPLETED';
  }

  get progress(): number {
    const total = this.cards.reduce((n, c) => n + c.rows.length, 0);
    const done = this.cards.reduce((n, c) => n + c.rows.filter((r) => r.done).length, 0);
    return total ? Math.round((done / total) * 100) : 0;
  }

  load(): void {
    this.loading = true;
    this.error = null;
    this.workouts.day(this.date, this.planDayId ? Number(this.planDayId) : undefined)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (d) => this.setDay(d),
        error: (err) => (this.error = apiErrorMessage(err, 'Could not load your workout.')),
      });
  }

  start(): void {
    if (!this.day) return;
    this.busy = true;
    this.workouts.start(this.day.date, this.day.planDayId)
      .pipe(finalize(() => (this.busy = false)))
      .subscribe((d) => {
        this.setDay(d);
        this.notify.success("Workout started - warm up for 5 minutes first.");
        this.changed.emit();
      });
  }

  save(card: ExerciseCard): void {
    if (!this.day?.sessionId) return;
    const sets: SetLogRequest[] = card.rows.map((r) => ({
      exerciseId: card.exercise.id,
      setNumber: r.setNumber,
      repsDone: card.timed ? null : r.reps,
      weightKg: card.timed ? null : r.weight,
      durationSec: card.timed ? r.seconds : null,
      completed: r.done,
    }));
    card.saving = true;
    this.workouts.logSets(this.day.sessionId, sets)
      .pipe(finalize(() => (card.saving = false)))
      .subscribe(() => (card.dirty = false));
  }

  toggleDone(card: ExerciseCard, row: SetRow): void {
    row.done = !row.done;
    card.dirty = true;
    this.save(card);
  }

  finish(): void {
    if (!this.day?.sessionId) return;
    const unsaved = this.cards.filter((c) => c.dirty);
    unsaved.forEach((c) => this.save(c));
    this.dialog.open<CompleteDialogComponent, number, CompleteResult>(CompleteDialogComponent, { data: this.progress, width: '440px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((res) => {
        if (!res || !this.day?.sessionId) return;
        this.busy = true;
        this.workouts.complete(this.day.sessionId, res.effort, res.notes)
          .pipe(finalize(() => (this.busy = false)))
          .subscribe((d) => {
            this.setDay(d);
            this.notify.success(`Great job! About ${d.caloriesEst ?? 0} kcal burned (estimate).`);
            this.changed.emit();
          });
      });
  }

  skip(): void {
    if (!this.day?.planDayId) return;
    const reason = prompt('Skipping is okay - rest matters. Optional: why are you skipping?') ;
    if (reason === null) return;
    this.workouts.skip(this.day.date, this.day.planDayId, reason || null).subscribe(() => {
      this.notify.info('Workout skipped. See you next session!');
      this.load();
      this.changed.emit();
    });
  }

  reschedule(): void {
    if (!this.day?.planDayId) return;
    this.dialog.open<RescheduleDialogComponent, RescheduleDialogData, boolean>(RescheduleDialogComponent, {
      data: { date: this.day.date, planDayId: this.day.planDayId, title: this.day.title },
      width: '400px', maxWidth: '95vw',
    }).afterClosed().subscribe((moved) => {
      if (moved) {
        this.load();
        this.changed.emit();
      }
    });
  }

  info(ex: ExerciseSummary): void {
    this.dialog.open(ExerciseDetailDialogComponent, { data: ex.id, width: '640px', maxWidth: '95vw' });
  }

  substitute(card: ExerciseCard): void {
    this.dialog.open<AlternativesDialogComponent, AlternativesDialogData, ExerciseSummary>(AlternativesDialogComponent, {
      data: { exercise: card.exercise, mode: 'session' }, width: '520px', maxWidth: '95vw',
    }).afterClosed().subscribe((alt) => {
      if (!alt) return;
      const replaced = card.substituteFor ?? card.exercise.name;
      card.exercise = alt;
      card.timed = alt.durationBased;
      card.substituteFor = replaced;
      card.rows = card.rows.map((r) => ({ ...r, done: false, seconds: alt.defaultDurationSec ?? r.seconds }));
      card.dirty = true;
      this.notify.info(`Swapped to ${alt.name} for this session.`);
    });
  }

  addSet(card: ExerciseCard): void {
    const last = card.rows[card.rows.length - 1];
    card.rows.push({ setNumber: card.rows.length + 1, reps: last?.reps ?? null, weight: last?.weight ?? null, seconds: last?.seconds ?? null, done: false });
    card.dirty = true;
  }

  target(e: SessionExercise): string {
    if (e.durationSec) return `${e.sets} × ${this.secs(e.durationSec)}`;
    return `${e.sets} × ${e.repsMin}${e.repsMax && e.repsMax !== e.repsMin ? '-' + e.repsMax : ''} reps`;
  }

  secs(s: number): string {
    return s >= 60 ? `${Math.floor(s / 60)} min${s % 60 ? ' ' + (s % 60) + 's' : ''}` : `${s}s`;
  }

  private setDay(d: DayWorkout): void {
    this.day = d;
    const fromPlan = d.exercises.map((e) => this.toCard(e));
    const extras = d.extraExercises.map((e) => ({ ...this.toCard(e), substituteFor: 'plan exercise' }));
    this.cards = [...fromPlan, ...extras];
  }

  private toCard(e: SessionExercise): ExerciseCard {
    const timed = e.exercise.durationBased || !!e.durationSec;
    const count = Math.max(e.sets, e.loggedSets.length);
    const rows: SetRow[] = [];
    for (let i = 1; i <= count; i++) {
      const logged = e.loggedSets.find((s) => s.setNumber === i);
      rows.push({
        setNumber: i,
        reps: logged?.repsDone ?? e.repsMax ?? e.repsMin ?? null,
        weight: logged?.weightKg ?? null,
        seconds: logged?.durationSec ?? e.durationSec ?? null,
        done: logged?.completed ?? false,
      });
    }
    return { source: e, exercise: e.exercise, timed, rows, dirty: false, saving: false };
  }
}
