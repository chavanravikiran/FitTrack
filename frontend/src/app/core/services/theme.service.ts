import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ThemePreference } from '../models/profile.models';

const STORAGE_KEY = 'ft-theme';

/**
 * Applies light/dark/system theme by toggling a class on <html>. The Material theme uses
 * CSS light-dark() driven by `color-scheme`, so switching is instant and needs no reload.
 * The preference is cached locally for a flicker-free start and synced to the profile.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly themeSubject = new BehaviorSubject<ThemePreference>(this.readStored());
  readonly theme$ = this.themeSubject.asObservable();

  constructor(@Inject(DOCUMENT) private readonly document: Document) {
    this.apply(this.themeSubject.value);
  }

  get theme(): ThemePreference {
    return this.themeSubject.value;
  }

  get isDark(): boolean {
    const t = this.themeSubject.value;
    if (t === 'SYSTEM') {
      return this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
    }
    return t === 'DARK';
  }

  setTheme(theme: ThemePreference): void {
    this.themeSubject.next(theme);
    this.apply(theme);
    try {
      localStorage.setItem(STORAGE_KEY, theme);
    } catch {
      /* storage unavailable (private mode) - theme still applies for this session */
    }
  }

  private apply(theme: ThemePreference): void {
    const root = this.document.documentElement;
    root.classList.remove('theme-light', 'theme-dark');
    if (theme === 'LIGHT') root.classList.add('theme-light');
    if (theme === 'DARK') root.classList.add('theme-dark');
  }

  private readStored(): ThemePreference {
    try {
      const v = localStorage.getItem(STORAGE_KEY);
      return v === 'LIGHT' || v === 'DARK' || v === 'SYSTEM' ? v : 'SYSTEM';
    } catch {
      return 'SYSTEM';
    }
  }
}
