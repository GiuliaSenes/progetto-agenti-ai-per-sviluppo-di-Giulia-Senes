package it.orizon.co2.chat;

import jakarta.persistence.*;
import java.time.Instant;

/** Singolo messaggio di una conversazione (memoria persistente). */
@Entity
@Table(name = "chat_message", indexes = @Index(columnList = "conversationId"))
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String conversationId;
    @Column(nullable = false)
    private String role; // user | assistant
    @Column(nullable = false, length = 8000)
    private String content;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Message() {}

    public Message(String conversationId, String role, String content) {
        this.conversationId = conversationId;
        this.role = role;
        this.content = content;
    }

    public Long getId() { return id; }
    public String getConversationId() { return conversationId; }
    public String getRole() { return role; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }
}
