package com.crmconnect.connection;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ConnectionService service;

    public ConnectionController(ConnectionService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ConnectionDto> list(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable) {
        return service.list(pageable);
    }

    @GetMapping("/{id}")
    public ConnectionDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ConnectionDto create(@Valid @RequestBody ConnectionRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ConnectionDto update(
            @PathVariable Long id,
            @Valid @RequestBody ConnectionRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/assign")
    public ConnectionDto assign(@PathVariable Long id, @RequestParam Long userId) {
        return service.assign(id, userId);
    }

    @GetMapping("/assignable-reps")
    public java.util.List<ConnectionService.RepView> assignableReps() {
        return service.assignableReps();
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
