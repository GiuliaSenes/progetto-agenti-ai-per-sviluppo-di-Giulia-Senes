package it.orizon.co2.chat;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByIdAsc(String conversationId);
    List<Message> findByConversationIdOrderByIdDesc(String conversationId, Pageable pageable);
}
