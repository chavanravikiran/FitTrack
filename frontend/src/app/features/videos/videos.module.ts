import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SharedModule } from '../../shared/shared.module';
import { VideoPlayerDialogComponent } from './video-player-dialog.component';
import { VideosPageComponent } from './videos-page.component';

@NgModule({
  declarations: [VideosPageComponent, VideoPlayerDialogComponent],
  imports: [SharedModule, RouterModule.forChild([{ path: '', component: VideosPageComponent }])],
})
export class VideosModule {}
