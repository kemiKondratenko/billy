package com.eugene.bill.demo.model;

import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;

import java.util.Map;

public class DetectionRequest {

    private String request_id;
    private Long chat_id;
    private String bucket_name;
    private String photo_name;
    private String type;

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

    public String getType() {
        return type;
    }

    public Map<String, Object> getRequest() {
        return request;
    }

    public void setRequest(Map<String, Object> request) {
        this.request = request;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DetectionRequest type(String type) {
        this.type = type;
        return this;
    }

    public DetectionRequest chatId(Long chat_id) {
        this.chat_id = chat_id;
        return this;
    }

    public DetectionRequest bucketName(String bucketName) {
        this.bucket_name = bucketName;
        return this;
    }

    public DetectionRequest photoName(String amazonPhotoName) {
        this.photo_name = amazonPhotoName;
        return this;
    }

    public DetectionRequest requestId(long requestId) {
        this.request_id = String.valueOf(requestId);
        return this;
    }

    public static DetectionRequest instantiate(String products_info_processing, DetectionResponse message) {
        DetectionRequest res = new DetectionRequest();
        BeanUtils.copyProperties(message, res);
        res.setType(products_info_processing);
        return res;
    }

    @Override
    public String toString() {
        return "DetectionRequest{" +
                "request_id='" + request_id + '\'' +
                ", chat_id=" + chat_id +
                ", bucket_name='" + bucket_name + '\'' +
                ", photo_name='" + photo_name + '\'' +
                ", type='" + type + '\'' +
                ", request='" + request + '\'' +
                '}';
    }
}
