package chatting.chatconsumer.domain.chatmessage.repository;

import chatting.chatconsumer.domain.chatmessage.document.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {
}
