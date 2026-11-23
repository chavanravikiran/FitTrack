import { DayOfWeek } from './assessment.models';

export type ReminderType = 'WAKE_UP' | 'WATER' | 'WORKOUT' | 'WALK' | 'MEAL' | 'WEIGHT' | 'SLEEP';
export type ReminderChannel = 'PUSH' | 'IN_APP' | 'EMAIL';

export interface Reminder {
  id: number;
  type: ReminderType;
  title: string;
  message?: string;
  time: string;
  days: DayOfWeek[];
  intervalMinutes?: number;
  endTime?: string;
  channel: ReminderChannel;
  timeZone: string;
  enabled: boolean;
  nextFireAt?: string;
  lastFiredAt?: string;
}

export interface ReminderRequest {
  type: ReminderType;
  title: string;
  message: string | null;
  time: string;
  days: DayOfWeek[];
  intervalMinutes: number | null;
  endTime: string | null;
  channel: ReminderChannel;
  enabled: boolean;
}

export interface AppNotification {
  id: number;
  title: string;
  body?: string;
  channel: ReminderChannel;
  read: boolean;
  createdAt: string;
  reminderId?: number;
  reminderType?: ReminderType;
}
