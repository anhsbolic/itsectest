package dev.harscode.itsectest.ports.service;

public interface LoginAttemptService {

    void assertNotBlocked(String key);

    void recordFailure(String key);

    void reset(String key);
}
