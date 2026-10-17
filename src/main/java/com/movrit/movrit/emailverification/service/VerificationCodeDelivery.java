package com.movrit.movrit.emailverification.service;

public interface VerificationCodeDelivery {
    void send(String email, String code, int expiresInMinutes);
}
