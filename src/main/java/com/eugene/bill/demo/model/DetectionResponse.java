package com.eugene.bill.demo.model;

import java.util.List;
import java.util.Map;

public class DetectionResponse {

    private String request_id;
    private Long chat_id;
    private String bucket_name;
    private String photo_name;
    private String type;


    private List<Prediction> boxes;
    private Map<String, Object> request;

    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public Long getChat_id() {
        return chat_id;
    }

    public void setChat_id(Long chat_id) {
        this.chat_id = chat_id;
    }

    public String getBucket_name() {
        return bucket_name;
    }

    public void setBucket_name(String bucket_name) {
        this.bucket_name = bucket_name;
    }

    public String getPhoto_name() {
        return photo_name;
    }

    public void setPhoto_name(String photo_name) {
        this.photo_name = photo_name;
    }

    public List<Prediction> getBoxes() {
        return boxes;
    }

    public void setBoxes(List<Prediction> boxes) {
        this.boxes = boxes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getRequest() {
        return request;
    }

    public void setRequest(Map<String, Object> request) {
        this.request = request;
    }

    @Override
    public String toString() {
        return "DetectionResponse{" +
                "request_id='" + request_id + '\'' +
                ", chat_id=" + chat_id +
                ", bucket_name='" + bucket_name + '\'' +
                ", photo_name='" + photo_name + '\'' +
                ", type='" + type + '\'' +
                ", boxes=" + boxes +
                ", request=" + request +
                '}';
    }
}
