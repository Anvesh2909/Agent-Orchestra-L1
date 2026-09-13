package backend.orchestra.agents;

import backend.orchestra.tasks.research.ResearchTask;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Order(1)
@Component
public class ResearchAgent implements AgentTask {
    private final List<ResearchTask> researchTasks;
    private final ExecutorService executorService;

    public ResearchAgent(List<ResearchTask> researchTasks, ExecutorService executorService) {
        this.researchTasks = researchTasks;
        this.executorService = executorService;
    }

    @Override
    public String name() {
        return "ResearchAgent";
    }

    @Override
    public String execute(String prompt) {
        List<Future<String>> futures = new ArrayList<>();
        for(ResearchTask task : researchTasks) {
            futures.add(executorService.submit(() -> {
                String response = task.execute(prompt);
                return task.name() + ": " + response;
            }));
        }
        StringBuilder combinedResponses = new StringBuilder();
        for (Future<String> future : futures) {
            try {
                String result = future.get(2, TimeUnit.SECONDS);
                combinedResponses.append(result).append("\n");
            } catch (TimeoutException e) {
                future.cancel(true);
                combinedResponses.append("Task timed out\n");
            } catch (ExecutionException e) {
                combinedResponses.append("Error executing task: ").append(e.getCause().getMessage()).append("\n");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                combinedResponses.append("Task interrupted\n");
                break;
            }
        }
        return combinedResponses.toString();
    }
}