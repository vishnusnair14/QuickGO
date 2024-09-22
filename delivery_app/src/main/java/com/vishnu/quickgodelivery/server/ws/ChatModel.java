package com.vishnu.quickgodelivery.server.ws;

public record ChatModel(String messageId, String content, String messageTime, boolean isSent) {
}
