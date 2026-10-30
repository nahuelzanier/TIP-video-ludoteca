package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.media.ImageValidation;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class GameUploadService {

    private static final long MAX_ARCHIVE_BYTES = 100L * 1024 * 1024;
    private static final long MAX_EXTRACTED_BYTES = 100L * 1024 * 1024;
    private static final long MAX_FILE_BYTES = 75L * 1024 * 1024;
    private static final long MAX_COVER_BYTES = 5L * 1024 * 1024;
    private static final int MAX_FILES = 2000;
    private static final String COVER_FILE_PATH = "__metadata__/thumbnail";

    private final GameRepository games;
    private final GameFileRepository gameFiles;
    private final UserRepository users;

    public GameUploadService(
            GameRepository games,
            GameFileRepository gameFiles,
            UserRepository users) {
        this.games = games;
        this.gameFiles = gameFiles;
        this.users = users;
    }

    @Transactional
    public UploadResult upload(
            String title,
            String description,
            MultipartFile cover,
            MultipartFile archive,
            String userEmail) {

        String cleanTitle = title == null ? "" : title.trim();
        String cleanDescription = description == null ? "" : description.trim();

        if (cleanTitle.isBlank() || cleanTitle.length() > 120) {
            throw badRequest("Title must contain between 1 and 120 characters.");
        }

        if (cleanDescription.length() > 5000) {
            throw badRequest("Description cannot exceed 5000 characters.");
        }

        StoredFile thumbnail = readThumbnail(cover);

        if (archive == null || archive.isEmpty()) {
            throw badRequest("Choose a ZIP file.");
        }

        if (archive.getSize() > MAX_ARCHIVE_BYTES) {
            throw badRequest("The ZIP file exceeds the 100 MB limit.");
        }

        User owner = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));

        String gameId = UUID.randomUUID().toString();

        Game game = new Game(
                gameId,
                owner,
                cleanTitle,
                cleanDescription,
                "/api/games/" + gameId + "/cover",
                "index.html",
                GameStatus.DRAFT
        );

        List<StoredFile> files = readArchive(archive);

        boolean hasEntryFile = files.stream()
                .anyMatch(file -> file.path().equals("index.html"));

        if (!hasEntryFile) {
            throw badRequest(
                    "The ZIP must contain index.html at its root. ZIP the contents of the game folder."
            );
        }

        boolean usesReservedCoverPath = files.stream()
                .anyMatch(file -> file.path().equals(COVER_FILE_PATH));

        if (usesReservedCoverPath) {
            throw badRequest("The ZIP uses a reserved path for the cover image.");
        }

        files.add(thumbnail);

        games.save(game);

        List<GameFile> rows = files.stream()
                .map(file -> new GameFile(
                        game,
                        file.path(),
                        file.contentType(),
                        file.content()
                ))
                .toList();

        gameFiles.saveAll(rows);

        return new UploadResult(game.getId(), game.getTitle(), rows.size());
    }

    private StoredFile readThumbnail(MultipartFile cover) {
        if (cover == null || cover.isEmpty()) {
            throw badRequest("Choose a cover image.");
        }

        if (cover.getSize() > MAX_COVER_BYTES) {
            throw badRequest("The cover image cannot exceed 5 MB.");
        }

        try {
            byte[] content = cover.getBytes();
            String contentType = ImageValidation.detectContentType(content);

            return new StoredFile(COVER_FILE_PATH, contentType, content);
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Could not read the cover image.",
                    exception
            );
        } catch (ImageValidation.RejectedImageException exception) {
            throw badRequest("The cover must be a PNG or JPEG image.");
        }
    }

    private List<StoredFile> readArchive(MultipartFile archive) {
        List<StoredFile> files = new ArrayList<>();
        Set<String> paths = new HashSet<>();
        long totalBytes = 0;

        try (InputStream raw = archive.getInputStream();
             ZipInputStream zip = new ZipInputStream(raw)) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                String path = validatePath(entry.getName());

                if (!paths.add(path)) {
                    throw badRequest("The ZIP contains a duplicate file path.");
                }

                if (files.size() >= MAX_FILES) {
                    throw badRequest("The ZIP contains too many files.");
                }

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                long fileBytes = 0;
                int read;

                while ((read = zip.read(buffer)) != -1) {
                    fileBytes += read;
                    totalBytes += read;

                    if (fileBytes > MAX_FILE_BYTES) {
                        throw badRequest("A file in the ZIP exceeds the size limit.");
                    }

                    if (totalBytes > MAX_EXTRACTED_BYTES) {
                        throw badRequest("The extracted ZIP exceeds the 100 MB limit.");
                    }

                    output.write(buffer, 0, read);
                }

                files.add(new StoredFile(
                        path,
                        detectContentType(path),
                        output.toByteArray()
                ));
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Could not read the ZIP file.",
                    exception
            );
        }

        if (files.isEmpty()) {
            throw badRequest("The ZIP contains no files.");
        }

        return files;
    }

    private String validatePath(String entryName) {
        if (entryName == null
                || entryName.isBlank()
                || entryName.startsWith("/")
                || entryName.contains("\\")
                || entryName.contains(":")) {
            throw badRequest("The ZIP contains an invalid file path.");
        }

        for (String part : entryName.split("/")) {
            if (part.isBlank() || part.equals(".") || part.equals("..")) {
                throw badRequest("The ZIP contains an invalid file path.");
            }
        }

        try {
            Path path = Paths.get(entryName).normalize();

            if (path.isAbsolute() || path.startsWith("..")) {
                throw badRequest("The ZIP contains an invalid file path.");
            }

            String normalized = path.toString().replace("\\", "/");

            if (normalized.isBlank() || normalized.length() > 500) {
                throw badRequest("A file path in the ZIP is too long.");
            }

            return normalized;
        } catch (RuntimeException exception) {
            if (exception instanceof ResponseStatusException responseException) {
                throw responseException;
            }

            throw badRequest("The ZIP contains an invalid file path.");
        }
    }

    private String detectContentType(String path) {
        String lowerPath = path.toLowerCase(Locale.ROOT);

        if (lowerPath.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lowerPath.endsWith(".js")) return "text/javascript; charset=UTF-8";
        if (lowerPath.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lowerPath.endsWith(".json")) return "application/json";
        if (lowerPath.endsWith(".wasm")) return "application/wasm";

        String guessed = URLConnection.guessContentTypeFromName(path);
        return guessed != null ? guessed : "application/octet-stream";
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private record StoredFile(String path, String contentType, byte[] content) {
    }

    public record UploadResult(String id, String title, int filesStored) {
    }
}