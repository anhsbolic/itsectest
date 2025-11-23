package dev.harscode.itsectest.security.permissions.article;

import dev.harscode.itsectest.web.exception.ForbiddenException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ArticlePermission {

    public ArticleScope resolveScope(UUID userId, String userRole, ArticleAction action) {
        return switch (userRole) {
            case "super_admin" -> ArticleScope.ALL;

            case "editor" -> switch (action) {
                case LIST, GET, CREATE, UPDATE, DELETE -> ArticleScope.OWN_ONLY;
            };

            case "contributor" -> switch (action) {
                case LIST, GET, CREATE, UPDATE -> ArticleScope.OWN_ONLY;
                case DELETE -> throw new ForbiddenException("Contributor cannot delete");
            };

            case "viewer" -> switch (action) {
                case LIST, GET -> ArticleScope.PUBLIC_ONLY;
                default -> throw new ForbiddenException("Viewer cannot modify");
            };

            default -> throw new ForbiddenException("Unknown role");
        };
    }
}
