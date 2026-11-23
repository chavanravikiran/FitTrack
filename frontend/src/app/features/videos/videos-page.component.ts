import { Component, DestroyRef, Input, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { Video, VideoCategory } from '../../core/models/video.models';
import { VideoService } from '../../core/services/video.service';
import { VIDEO_CATEGORIES } from '../../shared/constants/labels';
import { VideoPlayerDialogComponent } from './video-player-dialog.component';

@Component({
  selector: 'ft-videos-page',
  standalone: false,
  template: `
    <ft-page-header title="Exercise Videos" icon="smart_display" subtitle="Learn correct technique with step-by-step guides." />

    <div class="filters">
      <mat-chip-listbox [value]="category" (change)="setCategory($event.value)" aria-label="Category">
        <mat-chip-option [value]="null">All</mat-chip-option>
        @for (c of categories; track c.value) {
          <mat-chip-option [value]="c.value"><mat-icon matChipAvatar>{{ c.icon }}</mat-icon>{{ c.label }}</mat-chip-option>
        }
      </mat-chip-listbox>
      <div class="row">
        <mat-form-field appearance="outline" subscriptSizing="dynamic" class="grow">
          <mat-label>Search</mat-label>
          <mat-icon matPrefix>search</mat-icon>
          <input matInput [formControl]="search" />
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Difficulty</mat-label>
          <mat-select [(value)]="difficulty" (selectionChange)="load()">
            <mat-option [value]="null">Any</mat-option>
            <mat-option value="BEGINNER">Beginner</mat-option>
            <mat-option value="INTERMEDIATE">Intermediate</mat-option>
            <mat-option value="ADVANCED">Advanced</mat-option>
          </mat-select>
        </mat-form-field>
      </div>
    </div>

    @if (videos === null) {
      <mat-progress-bar mode="indeterminate" />
    } @else if (!videos.length) {
      <ft-empty-state icon="video_library" title="No videos match" message="Try a different category or search." />
    } @else {
      <div class="grid">
        @for (v of videos; track v.id) {
          <button class="card" type="button" (click)="open(v)" [attr.aria-label]="'Open ' + v.title">
            <div class="thumb" [style.background-image]="v.thumbnailUrl ? 'url(' + v.thumbnailUrl + ')' : null">
              @if (!v.thumbnailUrl) { <mat-icon class="thumb__icon" aria-hidden="true">{{ icon(v.category) }}</mat-icon> }
              <span class="play" aria-hidden="true"><mat-icon>{{ v.playable ? 'play_arrow' : 'menu_book' }}</mat-icon></span>
            </div>
            <div class="body">
              <strong>{{ v.title }}</strong>
              <span class="ft-muted small">{{ label(v.category) }} · {{ v.difficulty | titlecase }}{{ v.equipment.length ? ' · ' + gear(v) : '' }}</span>
              @if (!v.playable) { <span class="small guide">Step-by-step guide</span> }
            </div>
          </button>
        }
      </div>
    }
  `,
  styles: `
    .filters { display: flex; flex-direction: column; gap: 12px; margin-bottom: 20px; }
    .row { display: flex; gap: 12px; flex-wrap: wrap; }
    .grow { flex: 1; min-width: 220px; }
    .grid { display: grid; gap: 16px; grid-template-columns: repeat(auto-fill, minmax(230px, 1fr)); }
    .card { all: unset; cursor: pointer; display: flex; flex-direction: column; border-radius: 20px; overflow: hidden;
            background: var(--mat-sys-surface-container-low); border: 1px solid var(--mat-sys-outline-variant); transition: transform .15s; }
    .card:hover { transform: translateY(-2px); }
    .card:focus-visible { outline: 2px solid var(--mat-sys-primary); outline-offset: 2px; }
    .thumb { position: relative; aspect-ratio: 16 / 9; display: grid; place-items: center; background-size: cover; background-position: center;
             background-color: var(--mat-sys-primary-container); }
    .thumb__icon { font-size: 48px; width: 48px; height: 48px; color: var(--mat-sys-on-primary-container); opacity: .7; }
    .play { position: absolute; right: 10px; bottom: 10px; width: 40px; height: 40px; border-radius: 50%; display: grid; place-items: center;
            background: var(--mat-sys-surface); color: var(--mat-sys-primary); box-shadow: 0 2px 8px rgba(0,0,0,.2); }
    .body { padding: 12px 14px 14px; display: flex; flex-direction: column; gap: 4px; }
    .small { font: var(--mat-sys-body-small); }
    .guide { color: var(--mat-sys-tertiary); }
  `,
})
export class VideosPageComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);
  readonly categories = VIDEO_CATEGORIES;
  readonly search = new FormControl('', { nonNullable: true });
  videos: Video[] | null = null;
  category: VideoCategory | null = null;
  difficulty: string | null = null;
  private pendingPlay?: number;

  @Input('category') set categoryInput(v: string | undefined) {
    this.category = (v as VideoCategory) ?? null;
  }

  @Input() set play(v: string | undefined) {
    this.pendingPlay = v ? Number(v) : undefined;
  }

  constructor(
    private readonly videoService: VideoService,
    private readonly dialog: MatDialog,
  ) {}

  ngOnInit(): void {
    this.search.valueChanges.pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef)).subscribe(() => this.load());
    this.load();
    if (this.pendingPlay) {
      this.videoService.get(this.pendingPlay).subscribe((v) => this.open(v));
    }
  }

  setCategory(c: VideoCategory | null): void {
    this.category = c ?? null;
    this.load();
  }

  load(): void {
    this.videoService.library({ category: this.category, difficulty: this.difficulty, q: this.search.value.trim() || null })
      .subscribe((v) => (this.videos = v));
  }

  open(v: Video): void {
    this.dialog.open(VideoPlayerDialogComponent, { data: v, width: '820px', maxWidth: '96vw' });
  }

  label(c: VideoCategory): string {
    return this.categories.find((x) => x.value === c)?.label ?? c;
  }

  icon(c: VideoCategory): string {
    return this.categories.find((x) => x.value === c)?.icon ?? 'smart_display';
  }

  gear(v: Video): string {
    return v.equipment.filter((e) => e !== 'YOGA_MAT').map((e) => e.toLowerCase().replace(/_/g, ' ')).join(', ') || 'mat';
  }
}
