package backend.orchestra.tasks.research;

import org.springframework.stereotype.Component;

@Component
public class TechnicalDetailsTask implements ResearchTask {
    @Override
    public String name() {
        return "TechnicalDetailsTask";
    }

    @Override
    public String execute(String prompt) {
        // Simulate some processing time
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "These are the technical details for the prompt: " + prompt;
    }
}
