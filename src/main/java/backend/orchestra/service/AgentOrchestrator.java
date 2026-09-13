package backend.orchestra.service;

import backend.orchestra.agents.AgentTask;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentOrchestrator {
    private final List<AgentTask> agents;

    public AgentOrchestrator(List<AgentTask> agents) {
        this.agents = agents;
    }

    public String orchestrate(String prompt) {
        StringBuilder combinedResponses = new StringBuilder();

        for (AgentTask agent : agents) {

            long start = System.currentTimeMillis();

            String response = agent.execute(prompt);

            long elapsed = System.currentTimeMillis() - start;

            System.out.println(
                    agent.name() + " took " + elapsed + " ms"
            );

            combinedResponses
                    .append(agent.name())
                    .append(": ")
                    .append(response)
                    .append("\n");
        }

        return combinedResponses.toString();
    }
}