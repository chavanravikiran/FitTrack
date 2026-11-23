import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthResponse } from '../models/auth.models';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  const authResponse: AuthResponse = {
    accessToken: 'token-1',
    tokenType: 'Bearer',
    expiresIn: 900,
    user: {
      id: 1,
      email: 'a@b.com',
      fullName: 'Asha',
      roles: ['ROLE_USER'],
      onboardingCompleted: false,
      emailVerified: false,
      planCode: 'PREMIUM',
      premium: true,
    },
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('keeps the access token in memory after login', () => {
    service.login({ email: 'a@b.com', password: 'Secret123' }).subscribe();
    const req = http.expectOne('/api/v1/auth/login');
    expect(req.request.withCredentials).toBeTrue();
    req.flush({ success: true, data: authResponse });

    expect(service.token).toBe('token-1');
    expect(service.isAuthenticated()).toBeTrue();
    expect(localStorage.getItem('token')).toBeNull();
  });

  it('shares a single refresh call between concurrent callers', () => {
    const tokens: string[] = [];
    service.refresh().subscribe((t) => tokens.push(t));
    service.refresh().subscribe((t) => tokens.push(t));

    const reqs = http.match('/api/v1/auth/refresh');
    expect(reqs.length).toBe(1);
    reqs[0].flush({ success: true, data: { ...authResponse, accessToken: 'token-2' } });

    expect(tokens).toEqual(['token-2', 'token-2']);
  });

  it('restoreSession resolves and clears state when refresh fails', async () => {
    const done = service.restoreSession();
    http.expectOne('/api/v1/auth/refresh').flush({ success: false }, { status: 401, statusText: 'Unauthorized' });
    await done;
    expect(service.isAuthenticated()).toBeFalse();
  });
});
