import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { environment } from '../../../../environments/environment';

/**
 * Terms of Service and Privacy Policy.
 * IMPORTANT: this is a starting template written for an Indian consumer SaaS (DPDP Act 2023, Razorpay).
 * Have it reviewed and completed by a qualified lawyer before launching publicly.
 */
@Component({
  selector: 'ft-legal',
  standalone: false,
  templateUrl: './legal.component.html',
  styleUrl: './legal.component.scss',
})
export class LegalComponent {
  readonly doc: 'terms' | 'privacy';
  readonly email = environment.supportEmail;
  readonly company = environment.companyName;
  readonly draft = !environment.production;
  readonly updated = 'November 2026';

  constructor(route: ActivatedRoute) {
    this.doc = route.snapshot.data['doc'];
  }
}
