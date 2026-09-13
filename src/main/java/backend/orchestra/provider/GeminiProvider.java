package backend.orchestra.provider;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class GeminiProvider implements LLMProvider{
    private final ChatClient chatClient;

    public GeminiProvider(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }
    @Override
    public String generate(String prompt) {
        return chatClient.prompt(prompt).call().content();
    }
}