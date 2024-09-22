package com.vishnu.quickgoorder.miscellaneous;

import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Environment;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public class Utils {
    public final static String LOG_TAG = "Utils";
    static final SecureRandom random = new SecureRandom();
    private static Vibrator vibrator;

    // Initialize the vibrator
    public static void initializeVibrator(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    public static String generateAudioRefID() {
        return "AUD_R_" + UUID.randomUUID().toString().replace('-', '_');
    }

    public static String generateOrderID() {
        return "ORD_I_" + UUID.randomUUID().toString().trim().replace('-', '_');
    }

    public static String generateRandomKey() {
        final String ALPHAS = "qwertyuiopasdfghjklzxcvbnm";
        final String NUM = "0123456789";

        StringBuilder combination = new StringBuilder();

        int halfLength = 8 / 2;

        // Append alphabets
        for (int i = 0; i < halfLength; i++) {
            int index = random.nextInt(ALPHAS.length());
            combination.append(ALPHAS.charAt(index));
        }

        // Append numbers
        for (int i = 0; i < halfLength; i++) {
            int index = random.nextInt(NUM.length());
            combination.append(NUM.charAt(index));
        }

        return shuffleString(combination.toString());
    }

    private static String shuffleString(String input) {
        char[] characters = input.toCharArray();
        for (int i = 0; i < characters.length; i++) {
            int randomIndex = random.nextInt(characters.length);
            char temp = characters[i];
            characters[i] = characters[randomIndex];
            characters[randomIndex] = temp;
        }
        return new String(characters);
    }

    public static String generateTimestamp() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault());
        return dateFormat.format(new Date());
    }

    public static void deleteAddressDataCacheFile(Context context) {
        try {
            File file = new File(context.getFilesDir(), "address_data.json");
            if (file.exists()) {
                boolean deleted = file.delete();
                if (deleted) {
                    Log.d(LOG_TAG, "Address data file deleted successfully");
                } else {
                    Log.e(LOG_TAG, "Failed to delete address data file");
                }
            } else {
                Log.d(LOG_TAG, "Address data file does not exist");
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error deleting address data file", e);
        }
    }

    public static boolean deleteAllDownloadedVoiceOrderCartFiles(Context context) {
        // Get the Downloads directory
        File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        String subfolderName = "orderByVoice";

        if (downloadsDir != null) {
            // Create a reference to the subfolder
            File subfolder = new File(downloadsDir, subfolderName);

            // Check if the subfolder exists
            if (subfolder.exists() && subfolder.isDirectory()) {
                // Get all files in the subfolder
                File[] files = subfolder.listFiles();

                if (files != null) {
                    boolean allDeleted = true;

                    // Iterate over all files and delete them
                    for (File file : files) {
                        if (file.isFile()) {
                            if (!file.delete()) {
                                allDeleted = false;
                                Log.d(LOG_TAG, "Failed to delete file: " + file.getAbsolutePath());
                            }
                        }
                    }

                    // Optionally delete the subfolder itself if it's empty
                    if (subfolder.listFiles() != null) {
                        if (Objects.requireNonNull(subfolder.listFiles()).length == 0) {
                            if (subfolder.delete()) {
                                Log.d(LOG_TAG, "Subfolder deleted successfully");
                            } else {
                                Log.d(LOG_TAG, "Failed to delete subfolder");
                                allDeleted = false;
                            }
                        }
                    }

                    return allDeleted;
                } else {
                    Log.d(LOG_TAG, "No files found in the subfolder");
                }
            } else {
                Log.d(LOG_TAG, "Subfolder does not exist or is not a directory");
            }
        }
        return false; // Return false if subfolder does not exist or deletion failed
    }


    // Method to delete a file from the Downloads directory
    public static boolean deleteDownloadedVoiceOrderCartFile(String fileName, Context context) {
        // Get the Downloads directory
        File downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS + "/orderByVoice");

        if (downloadsDir != null) {
            // Create a file reference to the specific file
            File fileToDelete = new File(downloadsDir, fileName);

            // Check if the file exists and delete it
            if (fileToDelete.exists()) {
                Log.d(LOG_TAG, "Audio file deleted from downloads");
                return fileToDelete.delete();
            }
        }
        return false; // Return false if file does not exist or deletion failed
    }

    public static void deleteVoiceOrderCacheFile(Context context, String docID, String shopID) {
        // Define the file inside the folder
        File file;

        if (shopID == null) {
            file = new File(context.getFilesDir(), "voice_orders/" + docID + "/0/voice_orders_data_" + docID + ".json");
        } else {
            file = new File(context.getFilesDir(), "voice_orders/" + docID + "/" + shopID + "/voice_orders_data_" + docID + ".json");
        }

        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.d(LOG_TAG, "File deleted successfully: " + file.getAbsolutePath());
            } else {
                Log.e(LOG_TAG, "Failed to delete file: " + file.getAbsolutePath());
            }
        } else {
            Log.d(LOG_TAG, "File does not exist: " + file.getAbsolutePath());
        }
    }


    private static void deleteFolder(File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteFolder(file);
                }
            }
        }
        boolean rs = folder.delete();
        Log.d(LOG_TAG, "Item deleted: " + rs);
    }

    public static void deleteVoiceOrdersFolder(Context context) {
        File folder = new File(context.getFilesDir(), "voice_orders");
        if (folder.exists()) {
            deleteFolder(folder);
            Log.d(LOG_TAG, "Folder and its contents deleted successfully");
        } else {
            Log.d(LOG_TAG, "Folder does not exist");
        }
    }

    public static boolean isLocationNotEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        // Check if either GPS or network provider is enabled ...
        return locationManager == null ||
                (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                        !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    public static void showLocationSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        context.startActivity(intent);
    }

    public static void vibrate(Context context, int millis, int amplitude) {
        if (millis == 0) {
            millis = 150;
        }

        if (amplitude == 0) {
            amplitude = 2;
        }

        if (vibrator == null) {
            initializeVibrator(context);
        }
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, amplitude));
        }
    }
}
