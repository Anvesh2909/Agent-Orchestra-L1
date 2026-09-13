package backend.orchestra.agents;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(3)
@Component
public class DocumentationAgent implements AgentTask{
    @Override
    public String name() {
        return "DocumentationAgent";
    }

    @Override
    public String execute(String prompt) {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "This is a documentation agent response for the prompt: " + prompt;
    }
}
