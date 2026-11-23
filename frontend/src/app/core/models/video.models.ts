import { Difficulty, MuscleGroup } from './workout.models';

export type VideoCategory = 'CHEST' | 'BACK' | 'LEGS' | 'SHOULDERS' | 'ARMS' | 'ABS' | 'CARDIO' | 'WARM_UP' | 'STRETCHING';
export type VideoProvider = 'YOUTUBE' | 'VIMEO' | 'SELF_HOSTED';

export interface Video {
  id: number;
  title: string;
  category: VideoCategory;
  difficulty: Difficulty;
  muscleGroup?: MuscleGroup;
  exerciseId?: number;
  exerciseName?: string;
  equipment: string[];
  instructions?: string;
  provider: VideoProvider;
  playable: boolean;
  playUrl?: string;
  thumbnailUrl?: string;
  licenseNote?: string;
  active: boolean;
}

export interface VideoRequest {
  title: string;
  category: VideoCategory;
  difficulty: Difficulty;
  exerciseId: number | null;
  muscleGroupCode: string | null;
  videoUrl: string | null;
  instructions: string | null;
  licenseNote: string | null;
  equipmentCodes: string[];
  active: boolean;
}

export interface UploadTarget {
  key: string;
  uploadUrl: string;
  method: string;
  contentType: string;
  maxBytes: number;
}
