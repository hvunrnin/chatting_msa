package chatting.chatproducer.kafka.consumer;

import chatting.chatproducer.domain.room.service.ChatRoomMergeService;
import chatting.chatproducer.kafka.dto.MergeEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MergeEventConsumer {

    private final ChatRoomMergeService chatRoomMergeService;

    @KafkaListener(
            topics = "merge-events",
            groupId = "merge-event-consumer-group",
            containerFactory = "mergeEventKafkaListenerContainerFactory"
    )
    public void consumeMergeEvent(MergeEventDTO event) {
        log.info("병합 이벤트 수신: eventType={}, mergeId={}, targetRoomId={}",
                event.getEventType(), event.getMergeId(), event.getTargetRoomId());

        try {
            switch (event.getEventType()) {
                case "MERGE_INITIATED":
                    handleMergeInitiated(event);
                    break;
                case "ROOMS_LOCKED":
                    handleRoomsLocked(event);
                    break;
                case "MESSAGES_MIGRATED":
                    handleMessagesMigrated(event);
                    break;
                case "USERS_MIGRATED":
                    handleUsersMigrated(event);
                    break;
                case "MERGE_COMPLETED":
                    handleMergeCompleted(event);
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
        log.info("병합 시작 처리: mergeId={}, targetRoomId={}, sourceRoomIds={}",
                event.getMergeId(), event.getTargetRoomId(), event.getSourceRoomIds());
        chatRoomMergeService.handleRoomsLocked(event);
    }

    private void handleRoomsLocked(MergeEventDTO event) {
        log.info("방 잠금 완료 처리: mergeId={}, targetRoomId={}",
                event.getMergeId(), event.getTargetRoomId());
        chatRoomMergeService.handleMessagesMigrated(event);
    }

    private void handleMessagesMigrated(MergeEventDTO event) {
        log.info("메시지 마이그레이션 완료 처리: mergeId={}, migratedCount={}",
                event.getMergeId(), event.getMigratedMessageCount());
        chatRoomMergeService.handleUsersMigrated(event);
    }

    private void handleUsersMigrated(MergeEventDTO event) {
        log.info("사용자 마이그레이션 완료 처리: mergeId={}, migratedCount={}",
                event.getMergeId(), event.getMigratedUserCount());
        chatRoomMergeService.handleMergeCompleted(event);
    }

    private void handleMergeCompleted(MergeEventDTO event) {
        log.info("병합 완료 처리: mergeId={}, totalMessages={}, totalUsers={}",
                event.getMergeId(), event.getTotalMigratedMessages(), event.getTotalMigratedUsers());
        // 병합 완료는 이미 ChatRoomMergeService에서 처리됨
    }

    private void handleMergeFailed(MergeEventDTO event) {
        log.error("병합 실패 처리: mergeId={}, reason={}, failedStep={}",
                event.getMergeId(), event.getFailureReason(), event.getFailedStep());
        chatRoomMergeService.handleMergeFailed(event);
    }
} 