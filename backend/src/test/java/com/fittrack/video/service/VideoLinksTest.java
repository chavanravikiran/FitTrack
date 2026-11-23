package com.fittrack.video.service;

import com.fittrack.video.entity.ExerciseVideo.Provider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VideoLinksTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            "https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=10",
            "https://youtu.be/dQw4w9WgXcQ?si=abc",
            "https://www.youtube.com/embed/dQw4w9WgXcQ",
            "https://www.youtube.com/shorts/dQw4w9WgXcQ",
            "www.youtube-nocookie.com/embed/dQw4w9WgXcQ"})
    void normalisesYoutubeLinksToPrivacyEnhancedEmbed(String url) {
        var parsed = VideoLinks.parse(url).orElseThrow();
        assertThat(parsed.provider()).isEqualTo(Provider.YOUTUBE);
        assertThat(parsed.embedUrl()).isEqualTo("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ");
    }

    @Test
    void parsesVimeoAndRejectsOtherSites() {
        assertThat(VideoLinks.parse("https://vimeo.com/123456789").orElseThrow().embedUrl())
                .isEqualTo("https://player.vimeo.com/video/123456789");
        assertThat(VideoLinks.parse("https://evil.example.com/watch?v=dQw4w9WgXcQ")).isEmpty();
        assertThat(VideoLinks.parse("javascript:alert(1)")).isEmpty();
    }

    @Test
    void derivesYoutubeThumbnail() {
        assertThat(VideoLinks.youtubeThumbnail("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ"))
                .isEqualTo("https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg");
    }
}
