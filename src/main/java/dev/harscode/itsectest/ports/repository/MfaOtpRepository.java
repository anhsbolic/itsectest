package dev.harscode.itsectest.ports.repository;

import dev.harscode.itsectest.domain.auth.MfaOtpSession;

import java.util.Optional;

public interface MfaOtpRepository {

    MfaOtpSession create(MfaOtpSession session);

    Optional<MfaOtpSession> findById(String id);

    void save(MfaOtpSession session);

    void delete(String id);
}
