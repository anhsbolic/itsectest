package dev.harscode.itsectest.security.context;

import org.springframework.stereotype.Component;

@Component
public class RequestContext {

    private static final ThreadLocal<RequestFingerprint> HOLDER = new ThreadLocal<>();

    public void set(RequestFingerprint fingerprint) {
        HOLDER.set(fingerprint);
    }

    public RequestFingerprint get() {
        return HOLDER.get();
    }

    public void clear() {
        HOLDER.remove();
    }
}
