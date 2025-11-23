package dev.harscode.itsectest.application.auditlog;

import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.ports.repository.AuditLogRepository;
import dev.harscode.itsectest.web.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AuditLogAdminService implements AuditLogAdminUsecase {

    private final AuditLogRepository repo;

    public AuditLogAdminService(AuditLogRepository repo) {
        this.repo = repo;
    }

    @Override
    public List<AuditLogResult> list(AuditLogFilter filter) {
        List<AuditLog> all = repo.findAll();

        return all.stream()
                .filter(log -> filter.userId() == null || filter.userId().equals(log.getUserId()))
                .filter(log -> filter.activity() == null || filter.activity().equals(log.getActivity()))
                .filter(log -> filter.success() == null || filter.success().equals(log.isSuccess()))
                .map(this::toResult)
                .toList();
    }

    @Override
    public AuditLogResult getById(UUID id) {
        AuditLog log = repo.findById(id).orElseThrow(() ->
                new NotFoundException("Audit log not found")
        );
        return toResult(log);
    }

    private AuditLogResult toResult(AuditLog l) {
        return new AuditLogResult(
                l.getId(),
                l.getUserId(),
                l.getActivity(),
                l.getEntityType(),
                l.getEntityId(),
                l.isSuccess(),
                l.getIpAddressHash(),
                l.getUserAgentHash(),
                l.getActivityTime(),
                l.getDescription()
        );
    }
}
