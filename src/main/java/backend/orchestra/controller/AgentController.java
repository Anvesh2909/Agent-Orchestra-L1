package backend.orchestra.controller;

import backend.orchestra.dto.AgentRequest;
import backend.orchestra.dto.AgentResponse;
import backend.orchestra.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/agent/run")
@RequiredArgsConstructor
public class AgentController {
    private final AgentService agentService;

    @PostMapping
    public AgentResponse agentResponse(@RequestBody AgentRequest agentRequest){
        return agentService.run(agentRequest);
    }
}