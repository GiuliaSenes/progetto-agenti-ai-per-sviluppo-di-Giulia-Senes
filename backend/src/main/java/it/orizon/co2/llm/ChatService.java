package it.orizon.co2.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.orizon.co2.chat.ChatController.ChatResponse;
import it.orizon.co2.chat.Message;
import it.orizon.co2.chat.MessageRepository;
import it.orizon.co2.config.LlmProperties;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/** Orchestrazione: memoria persistente (DB) + system prompt + messaggio utente + LLM con tool. */
@Service
public class ChatService {

    private final MessageRepository repo;
    private final LlmClient llm;
    private final LlmProperties props;
    private final ObjectMapper mapper;

    public ChatService(MessageRepository repo, LlmClient llm, LlmProperties props, ObjectMapper mapper) {
        this.repo = repo;
        this.llm = llm;
        this.props = props;
        this.mapper = mapper;
    }

    public ChatResponse chat(String conversationId, String userText) {
        String id = (conversationId == null || conversationId.isBlank()) ? UUID.randomUUID().toString() : conversationId;

        List<Message> history = new ArrayList<>(repo.findByConversationIdOrderByIdDesc(id, PageRequest.of(0, props.historyLimit())));
        Collections.reverse(history);

        ArrayNode messages = mapper.createArrayNode();
        messages.add(msg("system", SystemPrompt.TEXT));
        history.forEach(m -> messages.add(msg(m.getRole(), m.getContent())));
        messages.add(msg("user", userText));

        String reply = llm.complete(messages);

        repo.save(new Message(id, "user", userText));
        repo.save(new Message(id, "assistant", reply.length() > 8000 ? reply.substring(0, 8000) : reply));
        return new ChatResponse(id, reply);
    }

    private ObjectNode msg(String role, String content) {
        return mapper.createObjectNode().put("role", role).put("content", content);
    }
}
