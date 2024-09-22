package com.vishnu.quickgoorder.ui.checkout;

// Helper class to hold shop details
public class Shop {
    private final String name;
    private final String preference;

    public Shop(String name, String preference) {
        this.name = name;
        this.preference = preference;
    }

    public String getName() {
        return name;
    }

    public String getPreference() {
        return preference;
    }
}