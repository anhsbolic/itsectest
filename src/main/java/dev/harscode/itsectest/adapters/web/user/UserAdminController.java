package dev.harscode.itsectest.adapters.web.user;

import dev.harscode.itsectest.adapters.web.user.dto.*;
import dev.harscode.itsectest.application.metadata.RequestMetadata;
import dev.harscode.itsectest.application.useradmin.UserAdminUsecase;
import dev.harscode.itsectest.web.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class UserAdminController {

    private final UserAdminUsecase usecase;
    private final UserWebMapper mapper;

    public UserAdminController(UserAdminUsecase usecase, UserWebMapper mapper) {
        this.usecase = usecase;
        this.mapper = mapper;
    }

    private RequestMetadata buildMetadata(HttpServletRequest req) {
        String ip = (String) req.getAttribute("fingerprint.ip");
        String ua = (String) req.getAttribute("fingerprint.ua");
        String actorId = (String) req.getAttribute("auth.userId");
        String actorRole = (String) req.getAttribute("auth.role");

        return new RequestMetadata(ip, ua, actorRole, actorId);
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        List<UserAdminResult> results = usecase.list(page, size);
        List<UserResponse> data = results.stream()
                .map(mapper::toResponse)
                .toList();

        return ApiResponse.ok("list of user", data);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable UUID id) {
        UserAdminResult result = usecase.get(id);
        return ApiResponse.ok("detail of user", mapper.toResponse(result));
    }

    @PostMapping
    public ApiResponse<UserResponse> create(
            @Valid @RequestBody CreateUserRequest body,
            HttpServletRequest request
    ) {
        CreateUserCommand cmd = new CreateUserCommand(
                body.username(),
                body.email(),
                body.password(),
                body.fullName(),
                body.role()
        );

        UserAdminResult result = usecase.create(cmd, buildMetadata(request));
        return ApiResponse.ok("user created", mapper.toResponse(result));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest body,
            HttpServletRequest request
    ) {
        UpdateUserCommand cmd = new UpdateUserCommand(
                body.email(),
                body.fullName(),
                body.role(),
                body.status(),
                body.mfaEnabled()
        );

        UserAdminResult result = usecase.update(id, cmd, buildMetadata(request));
        return ApiResponse.ok("user updated", mapper.toResponse(result));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        usecase.delete(id, buildMetadata(request));
        return ApiResponse.ok("user deleted", null);
    }
}