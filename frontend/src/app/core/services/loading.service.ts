import { Injectable } from '@angular/core';
import { BehaviorSubject, debounceTime, distinctUntilChanged, map } from 'rxjs';

/** Counts in-flight HTTP requests to drive the global progress bar. */
@Injectable({ providedIn: 'root' })
export class LoadingService {
  private readonly active = new BehaviorSubject<number>(0);

  /** Debounced so very fast requests don't flash the bar. */
  readonly loading$ = this.active.pipe(
    map((n) => n > 0),
    distinctUntilChanged(),
    debounceTime(120),
  );

  start(): void {
    this.active.next(this.active.value + 1);
  }

  stop(): void {
    this.active.next(Math.max(0, this.active.value - 1));
  }
}
