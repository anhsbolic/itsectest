package dev.harscode.itsectest.web.exception;

public class TooManyOtpAttemptsException extends RuntimeException {

    public TooManyOtpAttemptsException(String message) {
        super(message);
    }
}
