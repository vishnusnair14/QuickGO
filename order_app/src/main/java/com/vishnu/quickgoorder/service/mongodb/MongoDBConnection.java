package com.vishnu.quickgoorder.service.mongodb;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

public class MongoDBConnection {
    private static final String CONNECTION_STRING = "mongodb://localhost:27017/";

    public static MongoClient getMongoClient() {
        return MongoClients.create(CONNECTION_STRING);
    }
}