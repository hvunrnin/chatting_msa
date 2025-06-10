package chatting.chatconsumer.kafka.consumer;

import chatting.chatconsumer.kafka.producer.KafkaSendMsgProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import chatting.chatconsumer.domain.chat.dto.ChatMessage;
import chatting.chatconsumer.domain.chatmessage.service.ChatMessageMongoService;
import chatting.chatconsumer.kafka.dto.ChatKafkaMessage;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaChatConsumer {

    private final ChatMessageMongoService chatMessageMongoService;
    private final KafkaSendMsgProducer kafkaSendMsgProducer;

    @KafkaListener(topics = "chat-message", groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(ChatKafkaMessage message) {
        log.info("Kafka 메시지 수신: {}", message);

        // 1. MongoDB 저장
        chatMessageMongoService.saveMessage(
                message.getRoomId(),
                message.getSender(),
                message.getMessage(),
                message.getTimestamp() != null ? message.getTimestamp() : Instant.now()
        );

//        // 2. WebSocket 전송 - ChatMessage로 변환
//        messagingTemplate.convertAndSend(
//                "/sub/chat/room/" + message.getRoomId(),
//                ChatMessage.builder()
//                        .roomId(message.getRoomId())
//                        .sender(message.getSender())
//                        .message(message.getMessage())
//                        .timestamp(message.getTimestamp())
//                        .messageType(
//                                message.getMessageType() != null
//                                        ? ChatMessage.MessageType.valueOf(message.getMessageType())
//                                        : ChatMessage.MessageType.TALK
//                        )
//                        .build()
//        );

        // 2. 저장 완료 후 다시 Kafka로 전송
        kafkaSendMsgProducer.send(message);
    }
}

