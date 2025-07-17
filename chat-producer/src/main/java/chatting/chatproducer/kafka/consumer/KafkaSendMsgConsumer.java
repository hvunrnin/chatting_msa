package chatting.chatproducer.kafka.consumer;

import chatting.chatproducer.domain.chat.dto.ChatMessage;
import chatting.chatproducer.kafka.dto.ChatKafkaMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaSendMsgConsumer {

    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    @KafkaListener(
            topics = "chat-message-sent",
            groupId = "#{T(java.util.UUID).randomUUID().toString()}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeSent(ChatKafkaMessage message) {
        String roomId = message.getRoomId();
        String key = "lastTimestamp:" + roomId;

        String lastTimestampStr = redisTemplate.opsForValue().get(key);
        boolean isOutOfOrder = false;

        if (lastTimestampStr != null) {
            Instant last = Instant.parse(lastTimestampStr);
            if (last.isAfter(message.getTimestamp())) {
                isOutOfOrder = true;
            }
        }

        if (isOutOfOrder) {
            log.warn("순서 꼬임 감지: roomId={}, last={}, current={}", roomId, lastTimestampStr, message.getTimestamp());
            messagingTemplate.convertAndSend("/sub/chat/room/" + roomId + "/refresh", "refresh");
            return;
        }

        // 정상 메시지 전송
        messagingTemplate.convertAndSend(
                "/sub/chat/room/" + roomId,
                ChatMessage.builder()
                        .roomId(roomId)
                        .sender(message.getSender())
                        .message(message.getMessage())
                        .timestamp(message.getTimestamp())
                        .messageType(ChatMessage.MessageType.valueOf(message.getMessageType()))
                        .build()
        );

        // Redis에 최신 timestamp 저장
        redisTemplate.opsForValue().set(key, message.getTimestamp().toString());
    }
}
