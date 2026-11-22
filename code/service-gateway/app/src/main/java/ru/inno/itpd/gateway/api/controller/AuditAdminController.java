package ru.inno.itpd.gateway.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.inno.itpd.gateway.api.dto.AuditDtos.AuditEntry;
import ru.inno.itpd.gateway.api.dto.PageResponse;
import ru.inno.itpd.gateway.core.service.AuditService;
import ru.inno.itpd.gateway.util.QueryParams;

import java.time.Instant;

@RestController
@RequestMapping("/admin/audit")
@Tag(name = "Admin: audit")
@SecurityRequirement(name = "HTTPBearer")
public class AuditAdminController {
    private final AuditService audit;

    public AuditAdminController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    @Operation(summary = "List Audit Entries", description = "Newest first. `actor` matches the admin's id (sub) or username.",
            operationId = "admin_list_audit")
    public PageResponse<AuditEntry> list(@RequestParam(required = false) String from,
                                         @RequestParam(required = false) String to,
                                         @RequestParam(required = false) String actor,
                                         @RequestParam(required = false) String action,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        QueryParams.page(page, size);
        Instant end = QueryParams.instant("to", to, Instant.now().plusSeconds(1));
        Instant start = QueryParams.instant("from", from, Instant.EPOCH);
        QueryParams.requireRange(start, end);
        return audit.list(start, end, QueryParams.blankToNull(actor), QueryParams.blankToNull(action), page, size);
    }
}
