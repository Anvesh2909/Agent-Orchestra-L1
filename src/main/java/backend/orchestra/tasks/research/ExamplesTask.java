package backend.orchestra.tasks.research;

import org.springframework.stereotype.Component;

@Component
public class ExamplesTask implements ResearchTask {
    @Override
    public String name() {
        return "ExamplesTask";
    }

    @Override
    public String execute(String prompt) {
        // Simulate some processing time
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Here are some examples for the prompt: " + prompt;
    }
}
