package com.example.ytdl;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.downloader.request.RequestVideoFileDownload;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.downloader.response.Response;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.formats.VideoWithAudioFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DownloadService {

    private static final String DOWNLOAD_DIR = "downloads";

    public File download(String videoUrl, String type) throws Exception {
        Files.createDirectories(Paths.get(DOWNLOAD_DIR));

        YoutubeDownloader downloader = new YoutubeDownloader();
        String videoId = extractVideoId(videoUrl);
        if (videoId == null) throw new IllegalArgumentException("Неверная ссылка на YouTube");

        // Получаем информацию о видео
        Response<VideoInfo> infoResp = downloader.getVideoInfo(new RequestVideoInfo(videoId));
        if (!infoResp.isSuccessful()) {
            throw new RuntimeException("Не удалось получить информацию о видео: " + infoResp.error().getMessage());
        }
        VideoInfo info = infoResp.data();
        String title = sanitize(info.details().title());

        Format format;
        if ("audio".equalsIgnoreCase(type)) {
            // Ищем аудио-формат с наибольшим битрейтом
            AudioFormat best = null;
            for (AudioFormat af : info.audioFormats()) {
                if (best == null || af.audioQuality() != null
                        && af.audioQuality().compareTo(best.audioQuality()) > 0) {
                    best = af;
                }
            }
            if (best == null) throw new RuntimeException("Аудио-формат не найден");
            format = best;
        } else {
            // Ищем видео с аудио в максимальном разрешении
            VideoWithAudioFormat best = null;
            for (VideoWithAudioFormat vf : info.videoWithAudioFormats()) {
                if (best == null || vf.height() > best.height()) {
                    best = vf;
                }
            }
            if (best == null) {
                // fallback — только видео
                VideoFormat vf = info.bestVideoFormat();
                if (vf == null) throw new RuntimeException("Видео-формат не найден");
                format = vf;
            } else {
                format = best;
            }
        }

        String ext = format.extension().value();
        Path target = Paths.get(DOWNLOAD_DIR, title + "." + ext);

        Response<File> fileResp = downloader.downloadVideoFile(
                new RequestVideoFileDownload(format).saveTo(new File(DOWNLOAD_DIR)).renameTo(title)
        );

        if (!fileResp.isSuccessful()) {
            throw new RuntimeException("Ошибка скачивания: " + fileResp.error().getMessage());
        }
        return fileResp.data();
    }

    private String extractVideoId(String url) {
        if (url == null) return null;
        try {
            if (url.contains("youtu.be/")) {
                return url.substring(url.lastIndexOf("youtu.be/") + 9, url.indexOf("?", url.lastIndexOf("youtu.be/")) > 0
                        ? url.indexOf("?", url.lastIndexOf("youtu.be/"))
                        : url.length());
            }
            if (url.contains("v=")) {
                int start = url.indexOf("v=") + 2;
                int end = url.indexOf("&", start);
                return end > 0 ? url.substring(start, end) : url.substring(start);
            }
            if (url.contains("/shorts/")) {
                int start = url.indexOf("/shorts/") + 8;
                int end = url.indexOf("?", start);
                return end > 0 ? url.substring(start, end) : url.substring(start);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}