package backend.orchestra.service;

import backend.orchestra.provider.LLMProvider;
import backend.orchestra.dto.AgentRequest;
import backend.orchestra.dto.AgentResponse;
import org.springframework.stereotype.Service;

@Service
public class AgentService {
    private final AgentOrchestrator agentOrchestrator;

    public AgentService(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    public AgentResponse run(AgentRequest request) {
        String result = agentOrchestrator.orchestrate(request.prompt());
        return new AgentResponse(result);
    }
}