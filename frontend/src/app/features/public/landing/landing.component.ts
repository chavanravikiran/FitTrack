import { Component, OnInit } from '@angular/core';
import { catchError, of } from 'rxjs';
import { BillingConfig, Plan } from '../../../core/models/billing.models';
import { BillingService } from '../../../core/services/billing.service';

@Component({
  selector: 'ft-landing',
  standalone: false,
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
})
export class LandingComponent implements OnInit {
  plans: Plan[] = [];
  config: BillingConfig | null = null;

  readonly features = [
    { icon: 'fitness_center', title: 'Workouts made for you', text: 'Plans built from your goal, experience, schedule, equipment and any injuries, with clear beginner-friendly instructions.' },
    { icon: 'directions_walk', title: 'Track every day', text: 'Steps, water, sleep and weight in seconds. Quick-add buttons and reminders keep you consistent.' },
    { icon: 'restaurant', title: 'Indian-friendly nutrition', text: 'Log roti, dal, rice, paneer, eggs and more. See estimated calories and protein, carbs and fat.' },
    { icon: 'trending_up', title: 'See real progress', text: 'Charts for weight, measurements, steps and workouts, with insights that explain what the numbers mean.' },
    { icon: 'health_and_safety', title: 'Safe by design', text: 'No crash diets or overtraining. Calorie targets never go below your resting needs and rest days are built in.' },
    { icon: 'dark_mode', title: 'Works everywhere', text: 'Fast on phones and desktops, with light and dark themes. Your data stays private to you.' },
  ];

  readonly faqs = [
    { q: 'Is FitTrack suitable for complete beginners?', a: 'Yes. Plans start gently, every exercise has step-by-step instructions, common mistakes and safety tips, and intensity increases only as you progress.' },
    { q: 'Do I need a gym?', a: 'No. Choose "home" and the equipment you have - even none. Bodyweight plans work great to start.' },
    { q: 'Does it count my steps automatically?', a: 'In the browser you enter steps manually (it takes seconds). Automatic sync with phone health apps requires a mobile companion app, which is planned.' },
    { q: 'What happens after the free trial?', a: 'You keep using FitTrack on the Free plan - nothing is charged automatically. Upgrade any time if you want Premium features.' },
    { q: 'Is this medical advice?', a: 'No. FitTrack provides general fitness education and estimates. If you have a medical condition, injury or are pregnant, consult a qualified professional first.' },
  ];

  constructor(private readonly billing: BillingService) {}

  ngOnInit(): void {
    this.billing.plans().pipe(catchError(() => of([] as Plan[]))).subscribe((p) => (this.plans = p));
    this.billing.config().pipe(catchError(() => of(null))).subscribe((c) => (this.config = c));
  }
}
