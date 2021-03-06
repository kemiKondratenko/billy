package com.eugene.bill.demo;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SimpleBot extends TelegramLongPollingBot {


    private ExecutorService executorService;


    @Value("${bot.name}")
    private String botName;
    @Value("${bot.token}")
    private String botToken;
    @Value("${bot.location}")
    private String location;

    private static final String REMOTE_FILE_NAME =
            "https://api.telegram.org/bot%S/%S";

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(4);
        if (System.getenv("TELEGRAM_ROOT") != null)
            location = System.getenv("TELEGRAM_ROOT");
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> process(update));
    }

    private void process(Update update) {
        try {
            if (!update.getMessage().hasPhoto()) return;
            String currentTime = String.valueOf(System.currentTimeMillis());
            java.io.File folder =
                    Paths.get(
                            location,
                            String.valueOf(
                                    update.getMessage().getFrom().getId())
                    ).toFile();
            folder.mkdir();
            java.io.File logFile =
                    Paths.get(
                            folder.getPath(),
                            currentTime + ".myD"
                    ).toFile();
            logFile.createNewFile();
            try (PrintWriter writer = new PrintWriter(logFile)) {
                PhotoSize photoSize = getPhoto(update);
                String photoPath = getFilePath(photoSize);
                java.io.File photo = downloadPhotoByFilePath(photoPath);
                java.io.File photoFile =
                        Paths.get(
                                folder.getPath(),
                                currentTime + photoPath.substring(photoPath.lastIndexOf("."), photoPath.length())
                        ).toFile();
                photoFile.createNewFile();
                FileUtils.copyFile(photo, photoFile);
                if (photoSize.getFilePath() != null) {
                    writer.println(
                            "file = " +
                                    String.format(
                                            REMOTE_FILE_NAME,
                                            getBotToken(),
                                            photoSize.getFilePath()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private PhotoSize getPhoto(Update update) {
        // Check that the update contains a message and the message has a photo
        if (update.hasMessage() && update.getMessage().hasPhoto()) {
            // When receiving a photo, you usually get different sizes of it
            List<PhotoSize> photos = update.getMessage().getPhoto();

            // We fetch the bigger photo
            return photos.stream()
                    .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                    .findFirst()
                    .orElse(null);
        }

        // Return null if not found
        return null;
    }

    public String getFilePath(PhotoSize photo) {
        Objects.requireNonNull(photo);

        if (photo.hasFilePath()) { // If the file_path is already present, we are done!
            return photo.getFilePath();
        } else { // If not, let find it
            // We create a GetFile method and set the file_id from the photo
            GetFile getFileMethod = new GetFile();
            getFileMethod.setFileId(photo.getFileId());
            try {
                // We execute the method using AbsSender::execute method.
                File file = execute(getFileMethod);
                // We now have the file_path
                return file.getFilePath();
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }

        return null; // Just in case
    }

    public java.io.File downloadPhotoByFilePath(String filePath) {
        try {
            // Download the file calling AbsSender::downloadFile method
            return downloadFile(filePath);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
