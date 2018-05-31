package com.eugene.bill.demo;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.eugene.bill.demo.model.DetectionResponse;
import com.eugene.bill.demo.model.PartialDetectionRequest;
import com.eugene.bill.demo.model.Prediction;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

//@Service
public class QueuesProcessor {

    @Value("${aws.bucket}")
    private String bucketName;
    @Value("${aws.region}")
    private String region;
    @Value("${aws.que.prod.url}")
    private String queUrl;

    @Value("${aws.que.prod.process.photo}")
    private String processPhotoQueue;
    @Value("${aws.que.prod.process.bill}")
    private String processBillQueue;
    @Value("${aws.que.prod.process.products_info}")
    private String processProductsInfoQueue;

    private ExecutorService executorService;
    private AmazonSQS amazonSQS;

    @PostConstruct
    public void init() {
        executorService = Executors.newFixedThreadPool(4);
        amazonSQS = AmazonSQSClientBuilder.standard().withRegion(region).build();
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
                amazonSQS.deleteMessage(queUrl, message.getReceiptHandle());
                DetectionResponse detectionResponse =
                        new Gson().fromJson(message.getBody(), DetectionResponse.class);

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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void processBillExtractionResults(DetectionResponse message) {
        for (Prediction prediction : message.getBoxes()) {
            if ("bill".equals(prediction.getClass_name())) {
                amazonSQS.sendMessage(
                        new SendMessageRequest(
                                processBillQueue,
                                Utils.toJson(
                                        PartialDetectionRequest.instantiate("bill_processing", message, prediction)
                                )
                        )
                );
            } else if ("products_info".equals(prediction.getClass_name())) {
                amazonSQS.sendMessage(
                        new SendMessageRequest(
                                processProductsInfoQueue,
                                Utils.toJson(
                                        PartialDetectionRequest.instantiate("products_info_processing", message, prediction)
                                )
                        )
                );
            }
        }
    }

    private void processBillProcessingResults(DetectionResponse message) {
        for (Prediction prediction : message.getBoxes()) {
            if ("products_info".equals(prediction.getClass_name())) {
                amazonSQS.sendMessage(
                        new SendMessageRequest(
                                processProductsInfoQueue,
                                Utils.toJson(
                                        PartialDetectionRequest.instantiate("products_info_processing", message, prediction)
                                )
                        )
                );
            }
        }
    }

    private void processProductsInfoProcessingResults(DetectionResponse message) {
        System.out.println(message);
    }


}
