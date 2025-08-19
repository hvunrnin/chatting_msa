package chatting.chatproducer.kafka.consumer;

import chatting.chatproducer.domain.room.service.ChatRoomMergeService;
import chatting.chatproducer.kafka.dto.MergeEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Slf4j
@Component
@RequiredArgsConstructor
public class MergeEventConsumer {

    private final ChatRoomMergeService chatRoomMergeService;

    @PostConstruct
    public void init() {
        log.info("=== MergeEventConsumer 초기화 시작 ===");
        log.info("Kafka 토픽: merge-events");
        log.info("Consumer Group: merge-event-consumer-group-debug, test-consumer-group");
        log.info("Container Factory: kafkaListenerContainerFactory");
        log.info("MergeEventConsumer 초기화 완료");
        log.info("=== MergeEventConsumer 초기화 완료 ===");
    }

    @KafkaListener(
            topics = "merge-events",
            groupId = "merge-event-consumer-group",
            containerFactory = "mergeEventKafkaListenerContainerFactory"
    )
    public void consumeMergeEvent(MergeEventDTO event) {
        log.info("=== 병합 이벤트 수신 시작 ===");
        log.info("병합 이벤트 수신: eventType={}, mergeId={}, targetRoomId={}",
                event.getEventType(), event.getMergeId(), event.getTargetRoomId());
        log.info("=== 병합 이벤트 수신 완료 ===");

        try {
            switch (event.getEventType()) {
                case "MERGE_INITIATED":
                    handleMergeInitiated(event);
                    break;
                case "ROOMS_LOCKED":
                    log.info("여긴?????");
                    handleRoomsLocked(event);
                    break;
                case "MESSAGES_MIGRATED":
                    handleMessagesMigrated(event);
                    break;
                case "USERS_MIGRATED":
                    handleUsersMigrated(event);
                    break;
                case "MERGE_FAILED":
                    handleMergeFailed(event);
                    break;
                default:
                    log.warn("알 수 없는 이벤트 타입: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("병합 이벤트 처리 실패: eventType={}, mergeId={}", 
                    event.getEventType(), event.getMergeId(), e);
        }
    }



    private void handleMergeInitiated(MergeEventDTO event) {
        log.info("=== 병합 시작 처리 시작 ===");
        log.info("병합 시작 처리: mergeId={}, targetRoomId={}, sourceRoomIds={}",
                event.getMergeId(), event.getTargetRoomId(), event.getSourceRoomIds());
        
        try {
            // MERGE_INITIATED 이벤트가 들어오면 방을 잠그는 작업을 수행
            chatRoomMergeService.handleRoomsLocked(event);
            log.info("=== 병합 시작 처리 완료 ===");
        } catch (Exception e) {
            log.error("=== 병합 시작 처리 실패 ===", e);
            throw e;
        }
    }

    private void handleRoomsLocked(MergeEventDTO event) {
        log.info("=== 방 잠금 완료 처리 시작 ===");
        log.info("방 잠금 완료 처리: mergeId={}, targetRoomId={}",
                event.getMergeId(), event.getTargetRoomId());
        
        try {
            // 방 잠금 완료 후 메시지 마이그레이션 시작
            chatRoomMergeService.handleMessagesMigrate(event);
            log.info("=== 방 잠금 완료 처리 완료 ===");
        } catch (Exception e) {
            log.error("=== 방 잠금 완료 처리 실패 ===", e);
            throw e;
        }
    }

    private void handleMessagesMigrated(MergeEventDTO event) {
        log.info("메시지 마이그레이션 완료 처리: mergeId={}, migratedCount={}",
                event.getMergeId(), event.getMigratedMessageCount());
        // 메시지 마이그레이션은 이미 완료되었으므로 바로 사용자 마이그레이션으로 진행
        chatRoomMergeService.handleUsersMigrate(event);
    }

    private void handleUsersMigrated(MergeEventDTO event) {
        log.info("사용자 마이그레이션 완료 처리: mergeId={}, migratedCount={}",
                event.getMergeId(), event.getMigratedUserCount());
        chatRoomMergeService.handleMergeCompleted(event);
    }

    private void handleMergeFailed(MergeEventDTO event) {
        log.error("병합 실패 처리: mergeId={}, reason={}, failedStep={}",
                event.getMergeId(), event.getFailureReason(), event.getFailedStep());
        chatRoomMergeService.handleMergeFailed(event);
    }
} 