package backend.orchestra.provider;


import org.springframework.stereotype.Component;


@Component
public class MockLLMProvider implements LLMProvider{
    @Override
    public String generate(String prompt) {
        return "This is a mock response for the prompt: " + prompt;
    }
}