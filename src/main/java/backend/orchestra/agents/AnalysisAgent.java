package backend.orchestra.agents;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
@Order(2)
@Component
public class AnalysisAgent implements AgentTask{
    @Override
    public String name() {
        return "AnalysisAgent";
    }

    @Override
    public String execute(String prompt) {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "This is an analysis agent response for the prompt: " + prompt;
    }
}
