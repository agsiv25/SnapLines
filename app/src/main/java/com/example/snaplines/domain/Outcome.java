package com.example.snaplines.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Outcome {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private double price;

    @JsonProperty("point")
    private double point;

    @JsonProperty("link")
    private String link;

    @JsonProperty("sid")
    private String sid;

    @JsonProperty("bet_limit")
    private String betLimit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public String getBetLimit() {
        return betLimit;
    }

    public void setBetLimit(String betLimit) {
        this.betLimit = betLimit;
    }

    public double getPoint() {
        return point;
    }

    public void setPoint(double point) {
        this.point = point;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
