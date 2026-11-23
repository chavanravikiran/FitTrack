package com.fittrack.video.service;

import com.fittrack.video.entity.ExerciseVideo.Provider;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses YouTube/Vimeo links into privacy-friendly embed URLs. */
public final class VideoLinks {

    private static final Pattern YOUTUBE = Pattern.compile(
            "^(?:https?://)?(?:www\\.|m\\.)?(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/|live/)|youtu\\.be/|youtube-nocookie\\.com/embed/)([A-Za-z0-9_-]{11})(?:[?&#].*)?$");
    private static final Pattern VIMEO = Pattern.compile(
            "^(?:https?://)?(?:www\\.|player\\.)?vimeo\\.com/(?:video/)?(\\d{6,12})(?:[/?#].*)?$");

    public record Parsed(Provider provider, String embedUrl, String videoId) {
    }

    private VideoLinks() {
    }

    public static Optional<Parsed> parse(String url) {
        if (url == null) return Optional.empty();
        String u = url.trim();
        Matcher yt = YOUTUBE.matcher(u);
        if (yt.matches()) {
            return Optional.of(new Parsed(Provider.YOUTUBE, "https://www.youtube-nocookie.com/embed/" + yt.group(1), yt.group(1)));
        }
        Matcher vm = VIMEO.matcher(u);
        if (vm.matches()) {
            return Optional.of(new Parsed(Provider.VIMEO, "https://player.vimeo.com/video/" + vm.group(1), vm.group(1)));
        }
        return Optional.empty();
    }

    /** YouTube's public thumbnail for an embed URL, if it is one. */
    public static String youtubeThumbnail(String embedUrl) {
        return parse(embedUrl).filter(p -> p.provider() == Provider.YOUTUBE)
                .map(p -> "https://i.ytimg.com/vi/" + p.videoId() + "/hqdefault.jpg").orElse(null);
    }
}
