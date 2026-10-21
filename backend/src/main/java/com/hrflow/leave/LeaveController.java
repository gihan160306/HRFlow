package com.hrflow.leave;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
@RestController
@RequestMapping({"/api/v1/pto-requests", "/api/v1/leaves", "/pto-requests"})
public class LeaveController {
 private final LeaveService service;
 public LeaveController(LeaveService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public LeaveResponse create(@Valid @RequestBody LeaveCreateRequest request,Authentication auth){return service.createDraft(auth.getName(),request);}
 @GetMapping("/mine") public List<LeaveResponse> mine(Authentication auth){return service.mine(auth.getName());}
 @PostMapping("/{id}/submit") public LeaveResponse submit(@PathVariable Long id,Authentication auth){return service.submit(auth.getName(),id);}
 @PatchMapping("/{id}") public LeaveResponse action(@PathVariable Long id,@RequestBody LeaveActionRequest req,Authentication auth){Set<String> roles=auth.getAuthorities().stream().map(a->a.getAuthority()).collect(Collectors.toSet());return service.action(auth.getName(),roles,id,req.action(),req.reason());}
 public record LeaveActionRequest(String action,String reason){}
}
