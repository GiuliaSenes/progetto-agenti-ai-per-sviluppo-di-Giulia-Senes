package it.orizon.co2.chat;

import it.orizon.co2.llm.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public record ChatRequest(String conversationId, @NotBlank @Size(max = 2000) String message) {}
    public record ChatResponse(String conversationId, String reply) {}
    public record MessageDto(String role, String content) {}

    private final ChatService chatService;
    private final MessageRepository messages;

    public ChatController(ChatService chatService, MessageRepository messages) {
        this.chatService = chatService;
        this.messages = messages;
    }

    /** Unico endpoint di conversazione: riceve il messaggio utente, lo passa all'LLM, risponde. */
    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest req) {
        return chatService.chat(req.conversationId(), req.message());
    }

    /** Recupero dello storico (per ricaricare la chat dopo un refresh). */
    @GetMapping("/{conversationId}/messages")
    public List<MessageDto> history(@PathVariable String conversationId) {
        return messages.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(m -> new MessageDto(m.getRole(), m.getContent())).toList();
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
