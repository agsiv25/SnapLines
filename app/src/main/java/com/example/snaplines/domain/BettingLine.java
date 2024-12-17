package com.example.snaplines.domain;

public class BettingLine {

    private String bookmaker;
    private String market;
    private String name;
    private String description;
    private int price;
    private double point;
    private String link;
    private String betLimit;

    public String getBookmaker() {
        return bookmaker;
    }

    public void setBookmaker(String bookmaker) {
        this.bookmaker = bookmaker;
    }

    public String getMarket() {
        return market;
    }

    public void setMarket(String market) {
        this.market = market;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public double getPoint() {
        return point;
    }

    public void setPoint(double point) {
        this.point = point;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getBetLimit() {
        return betLimit;
    }

    public void setBetLimit(String betLimit) {
        this.betLimit = betLimit;
    }
}
