package com.vishnu.quickgoorder.service;

import org.bson.Document;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.vishnu.quickgoorder.service.mongodb.MongoDBConnection;

import java.util.List;

public class MongoDbHelper {

    private static final String DATABASE_NAME = "VoiGO";
    private static final String COLLECTION_NAME = "VoiceOrderData";

    public void addVoiceOrderToMongoDB(String userID, String orderDocID, String voiceOrderID, String key,
                                       String downloadUrl, Context context) {

        // Initialize MongoDB Realm Client
        MongoClient mongoClient = MongoDBConnection.getMongoClient();
        MongoDatabase mongoDatabase = mongoClient.getDatabase(DATABASE_NAME);
        MongoCollection<Document> collection = mongoDatabase.getCollection(COLLECTION_NAME);

        // Query filter to check if the document already exists
        Document queryFilter = new Document("user_id", userID)
                .append("order_by_voice_type", "obv")
                .append("voice_order_ref_id", voiceOrderID)
                .append("order_id", orderDocID);

        // Create the voice order data document to insert/update
        Document voiceOrderDocument = new Document("audio_key", key)
                .append("audio_storage_url", downloadUrl)
                .append("audio_title", "Timestamp here"); // You can replace this with a generated timestamp

        try {
            // Check if the document exists
            Document existingDocument = collection.find(queryFilter).first();
            if (existingDocument != null) {
                // Document exists, update it by adding the new voice order data
                Document updateQuery = new Document("$push", new Document("voice_order_data", voiceOrderDocument));
                collection.updateOne(queryFilter, updateQuery);
                Log.d("MongoDbHelper", "Audio URL updated in MongoDB for existing document.");
            } else {
                // Document does not exist, create a new one
                Document newDocument = new Document("user_id", userID)
                        .append("order_by_voice_type", "obv")
                        .append("order_id", orderDocID)
                        .append("voice_order_ref_id", voiceOrderID)
                        .append("voice_order_data", List.of(voiceOrderDocument));

                collection.insertOne(newDocument);
                Log.d("MongoDbHelper", "Audio URL uploaded to MongoDB as a new document.");
            }
        } catch (Exception e) {
            Log.e("MongoDbHelper", "MongoDB error: " + e.getMessage());
            Toast.makeText(context, "MongoDB error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
