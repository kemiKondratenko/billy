package com.eugene.bill.demo.model;

public class Prediction {

    private String class_name;

    private double score;

    private double x_max;
    private double x_min;
    private double y_max;
    private double y_min;


    public String getClass_name() {
        return class_name;
    }

    public void setClass_name(String class_name) {
        this.class_name = class_name;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

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

    @Override
    public String toString() {
        return "Prediction{" +
                "class_name='" + class_name + '\'' +
                ", score=" + score +
                ", x_max=" + x_max +
                ", x_min=" + x_min +
                ", y_max=" + y_max +
                ", y_min=" + y_min +
                '}';
    }
}
