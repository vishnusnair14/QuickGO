package com.vishnu.quickgoorder.server.ws;

public record ChatModel(String messageId, String content, String messageTime, boolean isSent) {
}
