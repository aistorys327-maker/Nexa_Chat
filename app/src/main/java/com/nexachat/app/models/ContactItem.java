package com.nexachat.app.models;

import java.io.Serializable;

public class ContactItem implements Serializable {
    private String name;
    private String phoneNumber;
    private String cleanPhone;
    private boolean onApp;
    private User user;

    public ContactItem() {
    }

    public ContactItem(String name, String phoneNumber, String cleanPhone, boolean onApp, User user) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.cleanPhone = cleanPhone;
        this.onApp = onApp;
        this.user = user;
    }

    public String getName() {
        return name != null ? name : "";
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber != null ? phoneNumber : "";
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getCleanPhone() {
        return cleanPhone != null ? cleanPhone : "";
    }

    public void setCleanPhone(String cleanPhone) {
        this.cleanPhone = cleanPhone;
    }

    public boolean isOnApp() {
        return onApp;
    }

    public void setOnApp(boolean onApp) {
        this.onApp = onApp;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
