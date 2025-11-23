package dev.harscode.itsectest.adapters.web.auditlog;

import dev.harscode.itsectest.application.auditlog.AuditLogAdminUsecase;
import dev.harscode.itsectest.application.auditlog.AuditLogFilter;

import dev.harscode.itsectest.web.dto.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AuditLogAdminController {

    private final AuditLogAdminUsecase usecase;

    public AuditLogAdminController(AuditLogAdminUsecase usecase) {
        this.usecase = usecase;
    }

    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(value = "userId", required = false) UUID userId,
            @RequestParam(value = "activity", required = false) String activity,
            @RequestParam(value = "success", required = false) Boolean success
    ) {
        var filter = new AuditLogFilter(userId, activity, success);
        return ApiResponse.ok("list of audit logs", usecase.list(filter));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> detail(@PathVariable("id") UUID id) {
        return ApiResponse.ok("detail of audit log", usecase.getById(id));
    }
}
