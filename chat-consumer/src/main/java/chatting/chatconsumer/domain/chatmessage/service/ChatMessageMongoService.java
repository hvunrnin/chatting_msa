package chatting.chatconsumer.domain.chatmessage.service;

import chatting.chatconsumer.domain.chatmessage.repository.ChatMessageMongoRepository;
import chatting.chatconsumer.domain.chatmessage.document.ChatMessageDocument;
import chatting.chatconsumer.domain.chatmessage.document.ChatMessageDocument.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class ChatMessageMongoService {

    private final ChatMessageMongoRepository chatMessageMongoRepository;

    public void saveMessage(String roomId, String sender, String content, Instant timestamp) {
        String dateKey = LocalDate.ofInstant(timestamp, ZoneOffset.UTC).toString(); // "2025-04-22"

        Message newMessage = Message.builder()
                .sender(sender)
                .message(content)
                .timestamp(timestamp)
                .build();

        ChatMessageDocument document = chatMessageMongoRepository
                .findById(roomId)
                .orElseGet(() -> ChatMessageDocument.builder()
                        .roomId(roomId)
                        .messagesByDate(new HashMap<>())
                        .build()
                );

        document.getMessagesByDate()
                .computeIfAbsent(dateKey, k -> new ArrayList<>())
                .add(newMessage);

        chatMessageMongoRepository.save(document);
    }

}
