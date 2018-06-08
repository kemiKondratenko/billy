package com.eugene.bill.demo.model;

import com.eugene.bill.demo.Utils;
import com.google.common.collect.Lists;
import org.springframework.beans.BeanUtils;

import java.util.LinkedList;
import java.util.Map;

public class PartialDetectionRequest extends DetectionRequest {

    private double x_max;
    private double x_min;
    private double y_max;
    private double y_min;


    public double getX_max() {
        return x_max;
    }

    public void setX_max(double x_max) {
        this.x_max = x_max;
    }

    public double getX_min() {
        return x_min;
    }

    public void setX_min(double x_min) {
        this.x_min = x_min;
    }

    public double getY_max() {
        return y_max;
    }

    public void setY_max(double y_max) {
        this.y_max = y_max;
    }

    public double getY_min() {
        return y_min;
    }

    public void setY_min(double y_min) {
        this.y_min = y_min;
    }

    public static PartialDetectionRequest instantiate(String type, DetectionResponse message, Prediction prediction) {
        PartialDetectionRequest partialDetectionRequest = new PartialDetectionRequest();
        BeanUtils.copyProperties(message, partialDetectionRequest);
        BeanUtils.copyProperties(prediction, partialDetectionRequest);
        partialDetectionRequest.setType(type);


        Map<String, Object> firstRequest = message.getRequest();

        if (firstRequest == null
                || !firstRequest.containsKey("x_max")
                || !firstRequest.containsKey("x_min")
                || !firstRequest.containsKey("y_max")
                || !firstRequest.containsKey("y_min"))
            return partialDetectionRequest;

        partialDetectionRequest.x_max = (double) firstRequest.get("x_max");
        partialDetectionRequest.x_min = (double) firstRequest.get("x_min");
        partialDetectionRequest.y_max = (double) firstRequest.get("y_max");
        partialDetectionRequest.y_min = (double) firstRequest.get("y_min");

        double prevWidth = partialDetectionRequest.x_max - partialDetectionRequest.x_min;
        double prevHeight = partialDetectionRequest.y_max - partialDetectionRequest.y_min;

        partialDetectionRequest.x_max -= (1 - prediction.getX_max()) * prevWidth;
        partialDetectionRequest.x_min += prediction.getX_min() * prevWidth;
        partialDetectionRequest.y_max -= (1 - prediction.getY_max()) * prevHeight;
        partialDetectionRequest.y_min += prediction.getY_min() * prevHeight;


        return partialDetectionRequest;
    }

    private static LinkedList<Map<String, Object>> getAllRequests(PartialDetectionRequest request) {
        LinkedList<Map<String, Object>> requests = Lists.newLinkedList();
        Map<String, Object> loc = Utils.fromJson(Utils.toJson(request));
        while (loc != null) {
            requests.addFirst(loc);
            loc = (Map<String, Object>) loc.get("request");
        }
        return requests;
    }

    @Override
    public String toString() {
        return "PartialDetectionRequest{" +
                "x_max=" + x_max +
                ", x_min=" + x_min +
                ", y_max=" + y_max +
                ", y_min=" + y_min +
                '}';
    }
}
