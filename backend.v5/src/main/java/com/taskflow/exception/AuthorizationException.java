package com.taskflow.exception;

public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) { super(message); }
}