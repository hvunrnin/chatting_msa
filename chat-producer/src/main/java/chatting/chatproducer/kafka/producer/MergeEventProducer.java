package chatting.chatproducer.kafka.producer;

import chatting.chatproducer.kafka.dto.MergeEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MergeEventProducer {
    
    private final KafkaTemplate<String, MergeEventDTO> kafkaTemplate;
    private static final String TOPIC_NAME = "merge-events";

    public void publishMergeInitiated(MergeEventDTO event) {
        publishEvent(event, "MERGE_INITIATED");
    }

    public void publishRoomsLocked(MergeEventDTO event) {
        publishEvent(event, "ROOMS_LOCKED");
    }

    public void publishMessagesMigrated(MergeEventDTO event) {
        publishEvent(event, "MESSAGES_MIGRATED");
    }

    public void publishUsersMigrated(MergeEventDTO event) {
        publishEvent(event, "USERS_MIGRATED");
    }

    public void publishMergeCompleted(MergeEventDTO event) {
        publishEvent(event, "MERGE_COMPLETED");
    }

    public void publishMergeFailed(MergeEventDTO event) {
        publishEvent(event, "MERGE_FAILED");
    }

    private void publishEvent(MergeEventDTO event, String eventType) {
        event.setEventType(eventType);

        try {
            var future = kafkaTemplate.send(TOPIC_NAME, event.getMergeId(), event);
            var result = future.get(); // 동기 대기
            
            log.info("병합 이벤트 발행 성공: eventType={}, mergeId={}, offset={}, partition={}",
                    eventType, event.getMergeId(),
                    result.getRecordMetadata().offset(),
                    result.getRecordMetadata().partition());
            log.info("=== 병합 이벤트 발행 완료 ===");
                    
        } catch (Exception e) {
            log.error("병합 이벤트 발행 실패: eventType={}, mergeId={}", eventType, event.getMergeId(), e);
            log.info("=== 병합 이벤트 발행 실패 ===");
            throw new RuntimeException("병합 이벤트 발행 실패", e);
        }
    }
} 