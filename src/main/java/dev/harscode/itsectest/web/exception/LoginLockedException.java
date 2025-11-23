package dev.harscode.itsectest.web.exception;

public class LoginLockedException extends RuntimeException {

    private final long retryAfterSeconds;

    public LoginLockedException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
