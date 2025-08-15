package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.room.entity.MergeStatus;
import chatting.chatproducer.domain.room.repository.MergeStatusRepository;
import chatting.chatproducer.kafka.dto.MergeEventDTO;
import chatting.chatproducer.kafka.producer.MergeEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomMergeService {

    private final MergeStatusRepository mergeStatusRepository;
    private final MergeEventProducer mergeEventProducer;
    private final MessageMigrationService messageMigrationService;
    private final UserMigrationService userMigrationService;
    private final MergeValidationService mergeValidationService;

    /**
     * 채팅방 병합 시작 (Saga 시작점)
     */
    @Transactional
    public String initiateMerge(String targetRoomId, List<String> sourceRoomIds, String initiatedBy) {
        String mergeId = UUID.randomUUID().toString();
        
        log.info("병합 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}, initiatedBy={}",
                mergeId, targetRoomId, sourceRoomIds, initiatedBy);

        try {
            // 1. 병합 상태 생성
            MergeStatus mergeStatus = MergeStatus.of(mergeId, targetRoomId, sourceRoomIds);
            mergeStatusRepository.save(mergeStatus);

            // 2. 병합 시작 이벤트 발행
            MergeEventDTO event = MergeEventDTO.builder()
                    .mergeId(mergeId)
                    .targetRoomId(targetRoomId)
                    .sourceRoomIds(sourceRoomIds)
                    .initiatedBy(initiatedBy)
                    .build();

            mergeEventProducer.publishMergeInitiated(event);

            log.info("병합 시작 완료: mergeId={}", mergeId);
            return mergeId;

        } catch (Exception e) {
            log.error("병합 시작 실패: mergeId={}, targetRoomId={}", mergeId, targetRoomId, e);
            publishMergeFailedEvent(mergeId, targetRoomId, sourceRoomIds, "INITIATED", e.getMessage());
            throw new RuntimeException("병합 시작 실패", e);
        }
    }

    /**
     * 방 잠금 처리
     */
    @Transactional
    public void handleRoomsLocked(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.info("방 잠금 처리 시작: mergeId={}", mergeId);

        try {
            // 1. 병합 상태 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.ROOMS_LOCKED);
            mergeStatusRepository.save(mergeStatus);

            // 2. 메시지 마이그레이션 시작
            messageMigrationService.migrateMessages(mergeId, event.getTargetRoomId(), event.getSourceRoomIds());

        } catch (Exception e) {
            log.error("방 잠금 처리 실패: mergeId={}", mergeId, e);
            publishMergeFailedEvent(mergeId, event.getTargetRoomId(), event.getSourceRoomIds(), "ROOMS_LOCKED", e.getMessage());
        }
    }

    /**
     * 메시지 마이그레이션 완료 처리
     */
    @Transactional
    public void handleMessagesMigrated(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.info("메시지 마이그레이션 완료 처리: mergeId={}, migratedCount={}", 
                mergeId, event.getMigratedMessageCount());

        try {
            // 1. 병합 상태 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.MESSAGES_MIGRATED);
            mergeStatusRepository.save(mergeStatus);

            // 2. 사용자 마이그레이션 시작
            userMigrationService.migrateUsers(mergeId, event.getTargetRoomId(), event.getSourceRoomIds());

        } catch (Exception e) {
            log.error("메시지 마이그레이션 완료 처리 실패: mergeId={}", mergeId, e);
            publishMergeFailedEvent(mergeId, event.getTargetRoomId(), event.getSourceRoomIds(), "MESSAGES_MIGRATED", e.getMessage());
        }
    }

    /**
     * 사용자 마이그레이션 완료 처리
     */
    @Transactional
    public void handleUsersMigrated(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.info("사용자 마이그레이션 완료 처리: mergeId={}, migratedCount={}", 
                mergeId, event.getMigratedUserCount());

        try {
            // 1. 병합 상태 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.USERS_MIGRATED);
            mergeStatusRepository.save(mergeStatus);

            // 2. 최종 검증 및 완료
            mergeValidationService.validateMerge(mergeId, event.getTargetRoomId(), event.getSourceRoomIds());

            // 3. 병합 완료 이벤트 발행
            MergeEventDTO completedEvent = MergeEventDTO.builder()
                    .mergeId(mergeId)
                    .targetRoomId(event.getTargetRoomId())
                    .sourceRoomIds(event.getSourceRoomIds())
                    .totalMigratedMessages(event.getMigratedMessageCount())
                    .totalMigratedUsers(event.getMigratedUserCount())
                    .build();

            mergeEventProducer.publishMergeCompleted(completedEvent);

        } catch (Exception e) {
            log.error("사용자 마이그레이션 완료 처리 실패: mergeId={}", mergeId, e);
            publishMergeFailedEvent(mergeId, event.getTargetRoomId(), event.getSourceRoomIds(), "USERS_MIGRATED", e.getMessage());
        }
    }

    /**
     * 병합 완료 처리
     */
    @Transactional
    public void handleMergeCompleted(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.info("병합 완료 처리: mergeId={}, totalMessages={}, totalUsers={}", 
                mergeId, event.getTotalMigratedMessages(), event.getTotalMigratedUsers());

        try {
            // 1. 병합 상태 완료로 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.COMPLETED);
            mergeStatus.setStatus("COMPLETED");
            mergeStatus.setCompletedAt(LocalDateTime.now());
            mergeStatusRepository.save(mergeStatus);

            log.info("병합 완료 처리 완료: mergeId={}", mergeId);

        } catch (Exception e) {
            log.error("병합 완료 처리 실패: mergeId={}", mergeId, e);
        }
    }

    /**
     * 병합 실패 처리
     */
    @Transactional
    public void handleMergeFailed(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.error("병합 실패 처리: mergeId={}, reason={}, failedStep={}", 
                mergeId, event.getFailureReason(), event.getFailedStep());

        try {
            // 1. 병합 상태 실패로 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setStatus("FAILED");
            mergeStatus.setFailureReason(event.getFailureReason());
            mergeStatus.setCompletedAt(LocalDateTime.now());
            mergeStatusRepository.save(mergeStatus);

            // 2. 롤백 처리 (필요시)
            // TODO: 롤백 로직 구현

            log.info("병합 실패 처리 완료: mergeId={}", mergeId);

        } catch (Exception e) {
            log.error("병합 실패 처리 중 오류: mergeId={}", mergeId, e);
        }
    }

    /**
     * 병합 상태 조회
     */
    public MergeStatus getMergeStatus(String mergeId) {
        return mergeStatusRepository.findById(mergeId)
                .orElseThrow(() -> new RuntimeException("병합 상태를 찾을 수 없습니다: " + mergeId));
    }

    /**
     * 병합 실패 이벤트 발행
     */
    private void publishMergeFailedEvent(String mergeId, String targetRoomId, List<String> sourceRoomIds, 
                                       String failedStep, String failureReason) {
        MergeEventDTO failedEvent = MergeEventDTO.builder()
                .mergeId(mergeId)
                .targetRoomId(targetRoomId)
                .sourceRoomIds(sourceRoomIds)
                .failedStep(failedStep)
                .failureReason(failureReason)
                .build();

        mergeEventProducer.publishMergeFailed(failedEvent);
    }
} 