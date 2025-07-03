package chatting.chatconsumer.domain.chatmessage.service;

import chatting.chatconsumer.domain.chatmessage.repository.ChatMessageMongoRepository;
import chatting.chatconsumer.domain.chatmessage.document.ChatMessageDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageMongoService {

    private final ChatMessageMongoRepository chatMessageMongoRepository;

    public void saveMessage(String roomId, String sender, String content, Instant timestamp) {

        ChatMessageDocument document = ChatMessageDocument.builder()
                .roomId(roomId)
                .sender(sender)
                .message(content)
                .timestamp(timestamp)
                .status("PENDING")
                .build();


        log.info("💡 저장 직전 상태: roomId={}, status={}", roomId, document.getStatus());

        chatMessageMongoRepository.save(document);
    }

}
