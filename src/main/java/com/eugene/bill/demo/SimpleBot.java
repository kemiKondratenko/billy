package com.eugene.bill.demo;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.methods.GetFile;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.File;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.PhotoSize;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
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
    @Value("${aws.bucket}")
    private String bucketName;

    private static final String FILE_PATH_FORMAT = "user_%s/photo-%s.jpg";

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(4);
    }

    private void uploadFileToS3(String fileName, java.io.File file) {
        String region = System.getenv("aws-region");
        AmazonS3 s3client = AmazonS3ClientBuilder.standard().withRegion(region).build();
        s3client.putObject(new PutObjectRequest(bucketName, fileName, file));
    }

    @Override
    public void onUpdateReceived(Update update) {
        executorService.submit(() -> process(update));
    }

    private void process(Update update) {
        String userName = null;
        long chatId = -1L;

        try {
            if (!update.hasMessage() && !update.getMessage().hasPhoto()) {
                return;
            }
            Message message = update.getMessage();
            chatId = message.getChatId();
            userName = message.getFrom().getUserName();
            String currentTime = String.valueOf(System.currentTimeMillis());
            Integer userId = message.getFrom().getId();
            PhotoSize photo = getPhoto(update);
            String photoPath = getFilePath(photo);
            java.io.File photoFile = downloadPhotoByFilePath(photoPath);
            uploadFileToS3(String.format(FILE_PATH_FORMAT, userId.toString(), currentTime), photoFile);
            SendMessage response = new SendMessage();
            response.setChatId(chatId).setText("Photo saved");
            execute(response);

        } catch (Exception e) {
            if (userName != null && chatId != -1 &&
                    (userName.equals("oteta_da") || userName.equals("evilkemi"))) {
                SendMessage response = new SendMessage();
                response.setChatId(chatId)
                        .setText(String.format("ERROR %s", e.getMessage()));
                try {
                    execute(response);
                } catch (TelegramApiException e1) {
                    e1.printStackTrace();
                }
            }
            e.printStackTrace();
        }

    }

    private PhotoSize getPhoto(Update update) {
        // When receiving a photo, you usually get different sizes of it
        List<PhotoSize> photos = update.getMessage().getPhoto();

        // We fetch the bigger photo
        return photos.stream()
                .sorted(Comparator.comparing(PhotoSize::getFileSize).reversed())
                .findFirst()
                .orElse(null);

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
