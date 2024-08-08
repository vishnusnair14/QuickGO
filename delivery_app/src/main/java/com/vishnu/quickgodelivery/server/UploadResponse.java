package com.vishnu.quickgodelivery.server;

public class UploadResponse {
    private String status;
    private String message;
    private String image_url;
    private String image_type;

    // Getters and setters
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getImageUrl() { return image_url; }
    public void setImageUrl(String image_url) { this.image_url = image_url; }

    public String getImageType() { return image_type; }
    public void setImageType(String image_type) { this.image_type = image_type; }
}
