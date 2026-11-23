import { Injectable } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

@Injectable({ providedIn: 'root' })
export class NotifyService {
  constructor(
    private readonly snackBar: MatSnackBar,
    private readonly router: Router,
  ) {}

  /** Plan-limit message with a shortcut to the upgrade page. */
  upgrade(message: string): void {
    this.snackBar
      .open(message, 'Upgrade', { duration: 8000 })
      .onAction()
      .subscribe(() => this.router.navigate(['/billing']));
  }

  success(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 3500, panelClass: 'ft-snack-success' });
  }

  error(message: string): void {
    this.snackBar.open(message, 'Dismiss', { duration: 6000, panelClass: 'ft-snack-error' });
  }

  info(message: string): void {
    this.snackBar.open(message, 'OK', { duration: 4000 });
  }
}
