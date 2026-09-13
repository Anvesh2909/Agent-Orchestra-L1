package backend.orchestra.tasks.research;

public interface ResearchTask {
    String name();
    String execute(String prompt);
}