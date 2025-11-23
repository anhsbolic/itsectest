package dev.harscode.itsectest.web.exception;

public class TooManyOtpRequestsException extends RuntimeException {

    public TooManyOtpRequestsException(String message) {
        super(message);
    }
}
