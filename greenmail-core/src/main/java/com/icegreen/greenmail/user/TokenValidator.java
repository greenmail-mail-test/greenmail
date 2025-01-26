package com.icegreen.greenmail.user;

public interface TokenValidator {
    boolean isValid(String token);
}
