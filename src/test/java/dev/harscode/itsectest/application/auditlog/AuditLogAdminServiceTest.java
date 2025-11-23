package dev.harscode.itsectest.application.auditlog;

import dev.harscode.itsectest.domain.audit.AuditLog;
import dev.harscode.itsectest.ports.repository.AuditLogRepository;
import dev.harscode.itsectest.web.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAdminServiceTest {

    @Mock
    private AuditLogRepository repo;

    @InjectMocks
    private AuditLogAdminService service;

    private UUID userId1;
    private UUID userId2;
    private UUID logId1;
    private UUID logId2;

    @BeforeEach
    void setUp() {
        userId1 = UUID.randomUUID();
        userId2 = UUID.randomUUID();
        logId1 = UUID.randomUUID();
        logId2 = UUID.randomUUID();
    }

    private AuditLog makeLog(UUID id, UUID userId, String activity, boolean success) {
        AuditLog l = new AuditLog();
        l.setId(id);
        l.setUserId(userId);
        l.setActivity(activity);
        l.setEntityType("User");
        l.setEntityId(UUID.randomUUID());
        l.setSuccess(success);
        l.setIpAddressHash("ip-hash");
        l.setUserAgentHash("ua-hash");
        l.setActivityTime(Instant.now());
        l.setDescription("desc " + activity);
        return l;
    }

    @Test
    void list_shouldReturnAll_whenFilterIsNull() {
        AuditLog l1 = makeLog(logId1, userId1, "LOGIN", true);
        AuditLog l2 = makeLog(logId2, userId2, "USER_CREATE", true);

        when(repo.findAll()).thenReturn(List.of(l1, l2));

        AuditLogFilter filter = new AuditLogFilter(null, null, null);

        List<AuditLogResult> results = service.list(filter);

        assertThat(results).hasSize(2);
        assertThat(results)
                .extracting(AuditLogResult::id)
                .containsExactlyInAnyOrder(logId1, logId2);

        verify(repo).findAll();
    }

    @Test
    void list_shouldFilterByUserIdActivityAndSuccess() {
        AuditLog l1 = makeLog(logId1, userId1, "LOGIN", true);
        AuditLog l2 = makeLog(UUID.randomUUID(), userId1, "LOGIN", false);
        AuditLog l3 = makeLog(logId2, userId2, "LOGIN", true);

        when(repo.findAll()).thenReturn(List.of(l1, l2, l3));

        AuditLogFilter filter = new AuditLogFilter(userId1, "LOGIN", true);

        List<AuditLogResult> results = service.list(filter);

        assertThat(results).hasSize(1);
        AuditLogResult r = results.get(0);
        assertThat(r.id()).isEqualTo(logId1);
        assertThat(r.userId()).isEqualTo(userId1);
        assertThat(r.activity()).isEqualTo("LOGIN");
        assertThat(r.success()).isTrue();

        verify(repo).findAll();
    }

    @Test
    void getById_shouldReturnResult_whenFound() {
        AuditLog log = makeLog(logId1, userId1, "LOGIN", true);

        when(repo.findById(logId1)).thenReturn(Optional.of(log));

        AuditLogResult result = service.getById(logId1);

        assertThat(result.id()).isEqualTo(logId1);
        assertThat(result.userId()).isEqualTo(userId1);
        assertThat(result.activity()).isEqualTo("LOGIN");
        assertThat(result.success()).isTrue();

        verify(repo).findById(logId1);
    }

    @Test
    void getById_shouldThrowNotFound_whenMissing() {
        when(repo.findById(logId1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(logId1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Audit log not found");

        verify(repo).findById(logId1);
        verifyNoMoreInteractions(repo);
    }
}