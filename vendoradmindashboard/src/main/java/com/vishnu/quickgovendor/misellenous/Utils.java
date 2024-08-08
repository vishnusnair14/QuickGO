package com.vishnu.quickgovendor.misellenous;

import java.util.Random;

public class Utils {

    public static String generateItemID() {
        String characters = "ABFG3CDE45HIJKTUYZ01VWX2NOPQR678LMS9";
        Random random = new Random();
        StringBuilder itemID = new StringBuilder();

        // Append 13 random characters
        for (int i = 0; i < 13; i++) {
            itemID.append(characters.charAt(random.nextInt(characters.length())));
        }

        return itemID.toString().toUpperCase();
    }
}
