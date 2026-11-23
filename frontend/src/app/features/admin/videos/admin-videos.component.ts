import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Video } from '../../../core/models/video.models';
import { NotifyService } from '../../../core/services/notify.service';
import { VideoService } from '../../../core/services/video.service';
import { VIDEO_CATEGORIES } from '../../../shared/constants/labels';
import { VideoEditDialogComponent } from './video-edit-dialog.component';

@Component({
  selector: 'ft-admin-videos',
  standalone: false,
  template: `
    <div class="head">
      <p class="ft-muted">Add YouTube/Vimeo links or upload your own videos to S3. Only use content you are licensed to share.</p>
      <button mat-flat-button (click)="edit()"><mat-icon>add</mat-icon> New video entry</button>
    </div>
    <mat-card appearance="outlined" class="table-card">
      <div class="table-wrap">
        <table mat-table [dataSource]="videos">
          <ng-container matColumnDef="title">
            <th mat-header-cell *matHeaderCellDef>Title</th>
            <td mat-cell *matCellDef="let v"><strong>{{ v.title }}</strong>@if (v.exerciseName) { <div class="ft-muted small">{{ v.exerciseName }}</div> }</td>
          </ng-container>
          <ng-container matColumnDef="category">
            <th mat-header-cell *matHeaderCellDef>Category</th>
            <td mat-cell *matCellDef="let v">{{ label(v.category) }}</td>
          </ng-container>
          <ng-container matColumnDef="media">
            <th mat-header-cell *matHeaderCellDef>Media</th>
            <td mat-cell *matCellDef="let v">
              @if (v.playable) { <span class="ok"><mat-icon inline>check_circle</mat-icon> {{ v.provider === 'SELF_HOSTED' ? 'Uploaded' : (v.provider | titlecase) }}</span> }
              @else { <span class="warn"><mat-icon inline>pending</mat-icon> Instructions only</span> }
            </td>
          </ng-container>
          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let v">{{ v.active ? 'Visible' : 'Hidden' }}</td>
          </ng-container>
          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef><span class="cdk-visually-hidden">Actions</span></th>
            <td mat-cell *matCellDef="let v">
              <button mat-icon-button (click)="edit(v)" [attr.aria-label]="'Edit ' + v.title"><mat-icon>edit</mat-icon></button>
              <button mat-icon-button (click)="remove(v)" [attr.aria-label]="'Delete ' + v.title"><mat-icon>delete</mat-icon></button>
            </td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"></tr>
        </table>
      </div>
    </mat-card>
  `,
  styles: `
    .head { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; margin-bottom: 16px; }
    .head p { margin: 0; }
    .table-card { border-radius: 20px; overflow: hidden; }
    .table-wrap { overflow-x: auto; }
    table { width: 100%; min-width: 640px; background: transparent; }
    .small { font: var(--mat-sys-body-small); }
    .ok { color: var(--mat-sys-primary); }
    .warn { color: var(--mat-sys-on-surface-variant); }
  `,
})
export class AdminVideosComponent implements OnInit {
  videos: Video[] = [];
  readonly columns = ['title', 'category', 'media', 'status', 'actions'];

  constructor(
    private readonly videoService: VideoService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  label(c: string): string {
    return VIDEO_CATEGORIES.find((x) => x.value === c)?.label ?? c;
  }

  edit(v?: Video): void {
    this.dialog.open(VideoEditDialogComponent, { data: v ?? null, width: '640px', maxWidth: '96vw' })
      .afterClosed().subscribe((changed) => changed && this.load());
  }

  remove(v: Video): void {
    if (!confirm(`Delete "${v.title}"? Uploaded files are removed from storage too.`)) return;
    this.videoService.delete(v.id).subscribe(() => {
      this.notify.success('Video entry deleted');
      this.load();
    });
  }

  private load(): void {
    this.videoService.adminList().subscribe((v) => (this.videos = v));
  }
}
