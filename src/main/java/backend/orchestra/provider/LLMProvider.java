package backend.orchestra.provider;

public interface LLMProvider {
    String generate(String prompt);
}