package com.duong.eventhub.exception;

public class ResourceExistedException extends RuntimeException {
    public ResourceExistedException(String message) {
        super(message);
    }
}
