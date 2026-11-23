import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { PageEvent } from '@angular/material/paginator';
import { debounceTime, distinctUntilChanged } from 'rxjs';
import { AdminUserRow, UserStatusFilter } from '../../../core/models/admin.models';
import { Plan } from '../../../core/models/billing.models';
import { AdminService } from '../../../core/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotifyService } from '../../../core/services/notify.service';
import { GrantDialogComponent, GrantDialogData } from './grant-dialog.component';
import { UserDetailDialogComponent } from './user-detail-dialog.component';

@Component({
  selector: 'ft-admin-users',
  standalone: false,
  templateUrl: './admin-users.component.html',
  styleUrl: './admin-users.component.scss',
})
export class AdminUsersComponent implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  readonly search = new FormControl('', { nonNullable: true });
  plan = '';
  status: UserStatusFilter = 'ANY';
  plans: Plan[] = [];

  rows: AdminUserRow[] = [];
  total = 0;
  page = 0;
  size = 20;
  loading = false;

  readonly columns = ['user', 'plan', 'status', 'joined', 'lastLogin', 'actions'];

  constructor(
    private readonly admin: AdminService,
    private readonly dialog: MatDialog,
    private readonly notify: NotifyService,
    readonly auth: AuthService,
  ) {}

  ngOnInit(): void {
    this.admin.plans().subscribe((p) => (this.plans = p));
    this.search.valueChanges
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.reload(true));
    this.reload();
  }

  reload(resetPage = false): void {
    if (resetPage) this.page = 0;
    this.loading = true;
    this.admin.users(this.search.value.trim(), this.plan, this.status, this.page, this.size).subscribe({
      next: (res) => {
        this.rows = res.content;
        this.total = res.totalElements;
        this.loading = false;
      },
      error: () => (this.loading = false),
    });
  }

  onPage(e: PageEvent): void {
    this.page = e.pageIndex;
    this.size = e.pageSize;
    this.reload();
  }

  view(row: AdminUserRow): void {
    this.dialog.open(UserDetailDialogComponent, { data: row.id, width: '720px', maxWidth: '95vw' });
  }

  grant(row: AdminUserRow): void {
    this.dialog
      .open<GrantDialogComponent, GrantDialogData, AdminUserRow>(GrantDialogComponent, {
        data: { user: row, plans: this.plans.filter((p) => !p.free) },
        width: '440px',
        maxWidth: '95vw',
      })
      .afterClosed()
      .subscribe((updated) => {
        if (updated) {
          this.notify.success(`${updated.planCode} granted to ${updated.email}`);
          this.replace(updated);
        }
      });
  }

  revoke(row: AdminUserRow): void {
    if (!confirm(`Move ${row.email} to the Free plan now? Any remaining paid or trial time is ended.`)) return;
    this.admin.revoke(row.id).subscribe((u) => {
      this.notify.success(`${u.email} moved to Free`);
      this.replace(u);
    });
  }

  toggleEnabled(row: AdminUserRow): void {
    const action = row.enabled ? 'Disable' : 'Enable';
    if (row.enabled && !confirm(`Disable ${row.email}? They will be signed out and unable to sign in.`)) return;
    this.admin.setEnabled(row.id, !row.enabled).subscribe((u) => {
      this.notify.success(`${action}d ${u.email}`);
      this.replace(u);
    });
  }

  isSelf(row: AdminUserRow): boolean {
    return row.id === this.auth.currentUser?.id;
  }

  private replace(updated: AdminUserRow): void {
    this.rows = this.rows.map((r) => (r.id === updated.id ? updated : r));
  }
}
