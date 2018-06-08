package com.eugene.bill.demo;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.eugene.bill.demo.model.DetectionResponse;
import com.eugene.bill.demo.model.PartialDetectionRequest;
import com.eugene.bill.demo.model.Prediction;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.api.methods.send.SendPhoto;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class QueuesProcessor {

    @Value("${aws.bucket}")
    private String bucketName;
    @Value("${aws.region}")
    private String region;
    @Value("${aws.que.prod.url}")
    private String queUrl;

    @Value("${bot.storage}")
    private String localStorage;

    @Value("${aws.que.prod.process.photo}")
    private String processPhotoQueue;
    @Value("${aws.que.prod.process.bill}")
    private String processBillQueue;
    @Value("${aws.que.prod.process.products_info}")
    private String processProductsInfoQueue;

    private ExecutorService executorService;
    private AmazonSQS amazonSQS;
    private AmazonS3 s3client;

    @Autowired
    private SimpleBot bot;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(4);
        amazonSQS = AmazonSQSClientBuilder.standard().withRegion(region).build();
        s3client = AmazonS3ClientBuilder.standard().withRegion(region).build();
        Executors.newSingleThreadExecutor().submit(
                () -> {
                    long sleepTime = 10;
                    while (true)
                        try {
                            ReceiveMessageResult messageResult = amazonSQS.receiveMessage(queUrl);
                            if (messageResult != null) {
                                executorService.submit(() -> processMessage(messageResult));
                                sleepTime = 10;
                            } else {
                                if (sleepTime < 1000) sleepTime += 5;
                                Thread.sleep(sleepTime);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }
        );
    }

    private void processMessage(ReceiveMessageResult messageResult) {
        try {
            for (Message message : messageResult.getMessages()) {
                DetectionResponse detectionResponse =
                        new Gson().fromJson(message.getBody(), DetectionResponse.class);

                try {
                    switch (detectionResponse.getType()) {
                        case "new":
                            amazonSQS.sendMessage(
                                    new SendMessageRequest(
                                            processPhotoQueue,
                                            message.getBody()
                                    )
                            );
                            break;
                        case "bill_extraction":
                            processBillExtractionResults(detectionResponse);
                            break;
                        case "bill_processing":
                            processBillProcessingResults(detectionResponse);
                            break;
                        case "products_info_processing":
                            processProductsInfoProcessingResults(detectionResponse);
                            break;
                        default:
                            throw new IllegalArgumentException("Not supported message type");

                    }
                    amazonSQS.deleteMessage(queUrl, message.getReceiptHandle());
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("Got an exception during processing " + detectionResponse);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processBillExtractionResults(DetectionResponse message) throws IOException, TelegramApiException {
        for (Prediction prediction : message.getBoxes()) {
            PartialDetectionRequest request = PartialDetectionRequest.instantiate("bill_extraction", message, prediction);
            if ("bill".equals(prediction.getClass_name())) {
                sendRes("Found bill during " + message.getType(), message, request);
                amazonSQS.sendMessage(
                        new SendMessageRequest(
                                processBillQueue,
                                Utils.toJson(
                                        request
                                )
                        )
                );
            } else if ("products_info".equals(prediction.getClass_name())) {
                sendRes("Found products info during "+message.getType(), message, request);
                amazonSQS.sendMessage(
                        new SendMessageRequest(
                                processProductsInfoQueue,
                                Utils.toJson(
                                        request
                                )
                        )
                );
            } else if ("summ".equals(prediction.getClass_name())) {
                sendRes("Found sum during "+message.getType(), message, request);
            }
        }
    }

    private void processBillProcessingResults(DetectionResponse message) throws IOException, TelegramApiException {
        for (Prediction prediction : message.getBoxes()) {
            PartialDetectionRequest request = PartialDetectionRequest.instantiate("bill_processing", message, prediction);
            if ("products_info".equals(prediction.getClass_name())) {
                sendRes("Found products info during " + message.getType(), message, request);
                amazonSQS.sendMessage(
                        new SendMessageRequest(
                                processProductsInfoQueue,
                                Utils.toJson(
                                        request
                                )
                        )
                );
            } else
                sendRes("Found " + prediction.getClass_name() + " during " + message.getType(), message, request);
        }
    }

    private void processProductsInfoProcessingResults(DetectionResponse message) throws IOException, TelegramApiException {
        for (Prediction prediction : message.getBoxes()) {
            PartialDetectionRequest request = PartialDetectionRequest.instantiate("products_info_processing", message, prediction);
            sendRes("Found " + prediction.getClass_name() + " during " + message.getType(), message, request);
        }
    }

    private void sendRes(String caption, DetectionResponse message, PartialDetectionRequest prediction) throws IOException, TelegramApiException {
        String fileName = String.format(localStorage, caption.toLowerCase().replace(" ", "_") + message.getPhoto_name());

//        System.out.println(caption + " " + Utils.toJson(prediction));

        downloadImage(fileName, message);
        cropImage(fileName, prediction);
        sendPhoto(caption, fileName, message.getChat_id());

        new File(fileName).deleteOnExit();

//        if (message.getRequest() != null && message.getRequest().get("request") != null) {
//            String request = Utils.toJson(message.getRequest());
//            DetectionResponse parentRequest = Utils.fromJson(request, DetectionResponse.class);
//            PartialDetectionRequest parentPrediction = Utils.fromJson(request, PartialDetectionRequest.class);
//
//
//            sendRes(caption + " prev prediction " + parentRequest.getType(), parentRequest, parentPrediction);
//        } else {
//            System.out.println("Finished");
//        }
    }


    private void downloadImage(String fileName, DetectionResponse message) {
        File localFile = new File(fileName);
        s3client.getObject(new GetObjectRequest(message.getBucket_name(), message.getPhoto_name()), localFile);
    }

    private void cropImage(String fileName, PartialDetectionRequest resPrediction) throws IOException {
        BufferedImage originalImage = ImageIO.read(new File(fileName));

        BufferedImage subImage =
                originalImage.getSubimage(
                        (int) (originalImage.getWidth() * resPrediction.getX_min()),
                        (int) (originalImage.getHeight() * resPrediction.getY_min()),
                        (int) (originalImage.getWidth() * (resPrediction.getX_max() - resPrediction.getX_min())),
                        (int) (originalImage.getHeight() * (resPrediction.getY_max() - resPrediction.getY_min())));

//        System.out.println("new image w:"+subImage.getWidth()+" h:"+subImage.getHeight() + " res prediction "+resPrediction);
        ImageIO.write(subImage, "jpg", new File(fileName));
    }


    private void sendPhoto(String caption, String fileName, Long chatId) throws TelegramApiException {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setCaption(caption);
        sendPhoto.setNewPhoto(new File(fileName));
        sendPhoto.setChatId(chatId);
        bot.sendPhoto(sendPhoto);
    }

}
