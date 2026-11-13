package ai.freightfox.optimizer.api;

import ai.freightfox.optimizer.service.OptimizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transporters")
public class OptimizationController {
    private final OptimizationService service;
    public OptimizationController(OptimizationService service){this.service=service;}
    @PostMapping("/input") @ResponseStatus(HttpStatus.CREATED)
    public ApiModels.InputResponse save(@Valid @RequestBody ApiModels.InputRequest request){return service.saveInput(request);}
    @PostMapping("/assignment")
    public ApiModels.AssignmentResponse assign(@Valid @RequestBody ApiModels.AssignmentRequest request){return service.assign(request);}
    @GetMapping("/assignment/latest")
    public ApiModels.AssignmentResponse latest(){return service.latest();}
}
