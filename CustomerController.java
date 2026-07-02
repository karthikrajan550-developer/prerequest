package com.petrolbunk.service;

/** Thrown when a business rule is violated (e.g. closing < opening). */
public class BusinessValidationException extends RuntimeException {
    public BusinessValidationException(String message) {
        super(message);
    }
}
