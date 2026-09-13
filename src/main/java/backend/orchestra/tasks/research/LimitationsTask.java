package backend.orchestra.tasks.research;

import org.springframework.stereotype.Component;

@Component
public class LimitationsTask implements ResearchTask {
    @Override
    public String name() {
        return "LimitationsTask";
    }

    @Override
    public String execute(String prompt) {
        // Simulate some processing time
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "These are the limitations for the prompt: " + prompt;
    }
}