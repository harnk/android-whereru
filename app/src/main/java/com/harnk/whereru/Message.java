package com.harnk.whereru;

import java.util.Date;

/**
 * Created by scottnull on 11/26/15.
 */
public class Message {

    private String senderName; // The sender of the message. If nil, the message was sent by the user.
    private String date;  // When the message was sent
    private String text;  // The text of the message
    private String location;  // The location of the sender
    private float distanceFromMeInMeters;  // The distance in meters of the sender from me
    // Determines whether this message was sent by the user of the app. We will
    // display such messages on the right-hand side of the screen.
    private boolean sentByUser;
    // This doesn't really belong in the data model, but we use it to cache the size of the speech bubble for this message.
    // CGSize bubbleSize;
    private float bubbleWidth;
    private float bubbleHeight;

    // Empty constructor
    public Message(){

    }

    public Message(String senderName, String date, String text, String location) {
        this.senderName = senderName;
        this.date = date;
        this.text = text;
        this.location = location;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;

    }

    public float getBubbleWidth() {
        return bubbleWidth;
    }

    public void setBubbleWidth(float bubbleWidth) {
        this.bubbleWidth = bubbleWidth;
    }

    public float getDistanceFromMeInMeters() {
        return distanceFromMeInMeters;
    }

    public void setDistanceFromMeInMeters(float distanceFromMeInMeters) {
        this.distanceFromMeInMeters = distanceFromMeInMeters;
    }

    public float getBubbleHeight() {
        return bubbleHeight;
    }

    public void setBubbleHeight(float bubbleHeight) {
        this.bubbleHeight = bubbleHeight;
    }

    public boolean isSentByUser() {
//        return sentByUser;
        return (senderName == null);
    }
}

