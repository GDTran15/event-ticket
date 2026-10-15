package com.duong.eventhub.user;

import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class RoleAdminController {

    private final AppUserRepository users;

    public RoleAdminController(AppUserRepository users) {
        this.users = users;
    }
    @PostMapping("/{id}/roles/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Set<Role> grantRole(@PathVariable UUID id, @PathVariable Role role) {
        if (role != Role.ORGANIZER && role != Role.STAFF)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only ORGANIZER or STAFF can be granted here");
        AppUser user = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.getRoles().add(role);
        return Set.copyOf(user.getRoles());
    }
}
