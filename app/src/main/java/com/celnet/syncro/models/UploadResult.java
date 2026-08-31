package com.celnet.syncro.models;

public class UploadResult {

    public final boolean success;
    public final String errorMessage;

    private UploadResult(boolean success, String errorMessage) {
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public static UploadResult ok() {
        return new UploadResult(true, null);
    }

    public static UploadResult error(String message) {
        return new UploadResult(false, message);
    }
}