package chatting.chatproducer.domain.chatmessage.repository;

import chatting.chatproducer.domain.chatmessage.document.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {
    List<ChatMessageDocument> findByRoomIdOrderByTimestampAsc(String roomId);
}