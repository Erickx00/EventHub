package com.eventhub.security;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller de teste, top-level de proposito: classe aninhada nao e registrada
 * pelo @WebMvcTest. Serve apenas para exercitar method security.
 */
@RestController
@RequestMapping("/api/organizer/protected")
public class ProtectedTestController {

    @GetMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public String organizerOnly() {
        return "ok";
    }
}
