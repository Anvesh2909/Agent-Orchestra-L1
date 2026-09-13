package backend.orchestra.agents;

public interface AgentTask {
    String name();
    String execute(String prompt);
}