package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.room.entity.ChatRoom;
import chatting.chatproducer.domain.room.entity.MergeStatus;
import chatting.chatproducer.domain.room.entity.RoomUser;
import chatting.chatproducer.domain.room.entity.UserMigrationLog;
import chatting.chatproducer.domain.room.repository.ChatRoomRepository;
import chatting.chatproducer.domain.room.repository.MergeStatusRepository;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
import chatting.chatproducer.domain.room.repository.UserMigrationLogRepository;
import chatting.chatproducer.kafka.dto.MergeEventDTO;
import chatting.chatproducer.kafka.producer.MergeEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    private final ChatRoomRepository chatRoomRepository;
    private final RoomUserRepository roomUserRepository;
    private final UserMigrationLogRepository userMigrationLogRepository;
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
        log.info("=== 방 잠금 처리 시작 ===");
        log.info("방 잠금 처리 시작: mergeId={}", mergeId);

        try {
            // 1. 병합 상태 조회 (트랜잭션 경계 문제 해결을 위해 잠시 대기)
            log.info("병합 상태 조회 대기 중...");
            Thread.sleep(100);
            
            log.info("병합 상태 조회 시작...");
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            log.info("병합 상태 조회 성공: currentStep={}, status={}", mergeStatus.getCurrentStep(), mergeStatus.getStatus());
            
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.ROOMS_LOCKED);
            mergeStatusRepository.save(mergeStatus);
            mergeStatusRepository.flush(); // 즉시 DB에 반영
            log.info("방 잠금 상태 업데이트 완료: mergeId={}", mergeId);

            log.info("방 잠금 완료: mergeId={}", mergeId);

            // 3. 방 잠금 완료 이벤트 발행
            MergeEventDTO roomlockedEvent = MergeEventDTO.builder()
                    .mergeId(mergeId)
                    .targetRoomId(event.getTargetRoomId())
                    .sourceRoomIds(event.getSourceRoomIds())
                    .build();

            mergeEventProducer.publishRoomsLocked(roomlockedEvent);

            log.info("=== 방 잠금 처리 완료 ===");

        } catch (Exception e) {
            log.error("=== 방 잠금 처리 실패 ===");
            log.error("방 잠금 처리 실패: mergeId={}", mergeId, e);
            publishMergeFailedEvent(mergeId, event.getTargetRoomId(), event.getSourceRoomIds(), "ROOMS_LOCKED", e.getMessage());
        }
    }

    /**
     * 메시지 마이그레이션 시작 처리
     */
    @Transactional
    public void handleMessagesMigrate(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.info("=== 메시지 마이그레이션 시작 ===");
        log.info("메시지 마이그레이션 시작: mergeId={}", mergeId);

        try {
            // 1. 병합 상태 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.MESSAGES_MIGRATED);
            mergeStatusRepository.save(mergeStatus);
            log.info("메시지 마이그레이션 상태 업데이트 완료: mergeId={}", mergeId);

            // 2. 메시지 마이그레이션 실행
            messageMigrationService.migrateMessages(mergeId, event.getTargetRoomId(), event.getSourceRoomIds());
            log.info("메시지 마이그레이션 완료: mergeId={}", mergeId);

            // 3. 메시지 마이그레이션 완료 이벤트 발행
            MergeEventDTO messagesMigratedEvent = MergeEventDTO.builder()
                    .mergeId(mergeId)
                    .targetRoomId(event.getTargetRoomId())
                    .sourceRoomIds(event.getSourceRoomIds())
                    .build();
            
            mergeEventProducer.publishMessagesMigrated(messagesMigratedEvent);
            log.info("메시지 마이그레이션 완료 이벤트 발행: mergeId={}", mergeId);

            log.info("=== 메시지 마이그레이션 완료 ===");

        } catch (Exception e) {
            log.error("=== 메시지 마이그레이션 실패 ===");
            log.error("메시지 마이그레이션 실패: mergeId={}", mergeId, e);
            publishMergeFailedEvent(mergeId, event.getTargetRoomId(), event.getSourceRoomIds(), "MESSAGES_MIGRATED", e.getMessage());
        }
    }

    /**
     * 사용자 마이그레이션 완료 처리
     */
    @Transactional
    public void handleUsersMigrate(MergeEventDTO event) {
        String mergeId = event.getMergeId();
        log.info("=== 사용자 마이그레이션 시작 ===");
        log.info("사용자 마이그레이션 시작: mergeId={}", mergeId);

        try {
            // 1. 병합 상태 업데이트
            MergeStatus mergeStatus = getMergeStatus(mergeId);
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.USERS_MIGRATED);
            mergeStatusRepository.save(mergeStatus);
            log.info("사용자 마이그레이션 상태 업데이트 완료: mergeId={}", mergeId);

            // 2. 사용자 마이그레이션 실행
            userMigrationService.migrateUsers(mergeId, event.getTargetRoomId(), event.getSourceRoomIds());
            log.info("사용자 마이그레이션 완료: mergeId={}", mergeId);

            // 3. 최종 검증
            mergeValidationService.validateMerge(mergeId, event.getTargetRoomId(), event.getSourceRoomIds());

            // 4. 사용자 마이그레이션 완료 이벤트 발행
            MergeEventDTO usersMigratedEvent = MergeEventDTO.builder()
                    .mergeId(mergeId)
                    .targetRoomId(event.getTargetRoomId())
                    .sourceRoomIds(event.getSourceRoomIds())
                    .totalMigratedMessages(event.getMigratedMessageCount())
                    .build();
            
            mergeEventProducer.publishUsersMigrated(usersMigratedEvent);
            log.info("사용자 마이그레이션 완료 이벤트 발행: mergeId={}", mergeId);

            log.info("=== 사용자 마이그레이션 완료 ===");

        } catch (Exception e) {
            log.error("=== 사용자 마이그레이션 실패 ===");
            log.error("사용자 마이그레이션 실패: mergeId={}", mergeId, e);
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
            mergeStatus.setStatus("COMPLETED");
            mergeStatus.setCurrentStep(MergeStatus.MergeStep.COMPLETED);
            mergeStatusRepository.save(mergeStatus);

            // 2. 소스 방들을 아카이브 상태로 변경
            for (String sourceRoomId : event.getSourceRoomIds()) {
                ChatRoom sourceRoom = chatRoomRepository.findById(sourceRoomId)
                    .orElseThrow(() -> new RuntimeException("소스 방을 찾을 수 없습니다: " + sourceRoomId));
                sourceRoom.archive();
                chatRoomRepository.save(sourceRoom);
                log.info("소스 방 아카이브 완료: roomId={}", sourceRoomId);
            }

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
            mergeStatusRepository.save(mergeStatus);

            // 2. 롤백 처리
            performRollback(mergeId, event.getFailedStep(), event.getTargetRoomId(), event.getSourceRoomIds());

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

    /**
     * 파싱 유틸 메서드
     */
    private java.util.Optional<RoomUser.UserRole> safeParseRole(String roleString) {
        if (roleString == null || roleString.trim().isEmpty()) {
            return java.util.Optional.empty();
        }
        
        try {
            return java.util.Optional.of(RoomUser.UserRole.valueOf(roleString.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 역할 문자열: {}", roleString);
            return java.util.Optional.empty();
        }
    }

    /**
     * 롤백 처리
     */
    private void performRollback(String mergeId, String failedStep, String targetRoomId, List<String> sourceRoomIds) {
        log.info("롤백 시작: mergeId={}, failedStep={}", mergeId, failedStep);

        try {
            switch (failedStep) {
                case "USERS_MIGRATED":
                    // 사용자 마이그레이션 롤백
                    rollbackUserMigration(mergeId);
                    // fall through
                case "MESSAGES_MIGRATED":
                    // 메시지 마이그레이션 롤백
                    rollbackMessageMigration(mergeId, targetRoomId, sourceRoomIds);
                    // fall through
                case "ROOMS_LOCKED":
                    // 방 잠금 해제
                    unlockRooms(mergeId, targetRoomId, sourceRoomIds);
                    break;
                case "INITIATED":
                    // 초기 상태로 롤백
                    unlockRooms(mergeId, targetRoomId, sourceRoomIds);
                    break;
                default:
                    log.warn("알 수 없는 실패 단계: {}", failedStep);
            }

            log.info("롤백 완료: mergeId={}", mergeId);

        } catch (Exception e) {
            log.error("롤백 처리 실패: mergeId={}", mergeId, e);
            throw new RuntimeException("롤백 처리 실패", e);
        }
    }

    /**
     * 사용자 마이그레이션 롤백
     */
    @Transactional
    public void rollbackUserMigration(String mergeId) {
        log.info("사용자 마이그레이션 롤백 시작: mergeId={}", mergeId);
        int page = 0, rollbackCount = 0;

        while (true) {
            List<UserMigrationLog> logs = userMigrationLogRepository.findByMergeIdAndStatus(
                mergeId, UserMigrationLog.MigrationStatus.MIGRATED, PageRequest.of(page++, 1000));
            if (logs.isEmpty()) break;

            log.debug("페이징 처리: mergeId={}, page={}, logCount={}", mergeId, page-1, logs.size());

            for (UserMigrationLog logRow : logs) {
                try {
                    if (logRow.isRolledBack()) continue; // 멱등

                    // 타겟 처리
                    boolean inTarget = roomUserRepository
                        .existsByRoomIdAndUserId(logRow.getTargetRoomId(), logRow.getUserId());

                    if (!logRow.isWasMemberInTo()) {
                        // 정방향에서 타겟에 "새로 추가"되었던 사용자 → 제거(멱등)
                        if (inTarget) {
                            roomUserRepository.deleteByRoomIdAndUserId(logRow.getTargetRoomId(), logRow.getUserId());
                            log.debug("타겟에서 사용자 제거: userId={}, targetRoomId={}", 
                                    logRow.getUserId(), logRow.getTargetRoomId());
                        }
                    } else if (logRow.getPrevRoleInTo() != null && inTarget) {
                        roomUserRepository.updateRole(
                            logRow.getUserId(), logRow.getTargetRoomId(), logRow.getPrevRoleInTo());
                        log.debug("타겟 사용자 역할 복원: userId={}, targetRoomId={}, prevRole={}", 
                                logRow.getUserId(), logRow.getTargetRoomId(), logRow.getPrevRoleInTo());
                    }

                    // 소스 복구
                    if (logRow.isWasMemberInFrom()) {
                        boolean inSource = roomUserRepository
                            .existsByRoomIdAndUserId(logRow.getSourceRoomId(), logRow.getUserId());

                        if (!inSource) {
                            var role = safeParseRole(logRow.getPrevRoleInFrom())
                                      .orElse(RoomUser.UserRole.MEMBER);
                            RoomUser sourceUser = RoomUser.builder()
                                    .roomId(logRow.getSourceRoomId())
                                    .userId(logRow.getUserId())
                                    .joinedAt(LocalDateTime.now())
                                    .role(role)
                                    .build();
                            roomUserRepository.save(sourceUser);
                            log.debug("소스 방에 사용자 복원: userId={}, sourceRoomId={}, role={}", 
                                    logRow.getUserId(), logRow.getSourceRoomId(), role.name());
                        } else if (logRow.getPrevRoleInFrom() != null) {
                            roomUserRepository.updateRole(
                                logRow.getUserId(), logRow.getSourceRoomId(), logRow.getPrevRoleInFrom());
                            log.debug("소스 사용자 역할 복원: userId={}, sourceRoomId={}, prevRole={}", 
                                    logRow.getUserId(), logRow.getSourceRoomId(), logRow.getPrevRoleInFrom());
                        }
                    }

                    logRow.markAsRolledBack();
                    userMigrationLogRepository.save(logRow);
                    rollbackCount++;

                } catch (Exception ex) {
                    log.error("개별 사용자 롤백 실패: mergeId={}, userId={}",
                              mergeId, logRow.getUserId(), ex);
                    // 재시도 테이블/알림 등 후속 처리 권장
                }
            }
        }

        log.info("사용자 마이그레이션 롤백 완료: mergeId={}, rollbackCount={}", mergeId, rollbackCount);
    }

    /**
     * 메시지 마이그레이션 롤백
     */
    private void rollbackMessageMigration(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("메시지 마이그레이션 롤백 시작: mergeId={}", mergeId);
        
        try {
            // TODO: 타겟 방에서 소스 방으로 메시지를 다시 이동
            
            // 현재는 로그만 남기고 실제 롤백은 구현하지 않음
            log.warn("메시지 마이그레이션 롤백은 복잡한 작업이므로 수동 처리 필요: mergeId={}", mergeId);
            
            log.info("메시지 마이그레이션 롤백 완료: mergeId={}", mergeId);
        } catch (Exception e) {
            log.error("메시지 마이그레이션 롤백 실패: mergeId={}", mergeId, e);
            throw new RuntimeException("메시지 마이그레이션 롤백 실패", e);
        }
    }

    /**
     * 방 잠금 해제
     */
    private void unlockRooms(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("방 잠금 해제 시작: mergeId={}", mergeId);
        
        try {
            // 타겟 방과 소스 방들의 상태를 ACTIVE로 변경
            
            // 타겟 방 잠금 해제
            ChatRoom targetRoom = chatRoomRepository.findById(targetRoomId)
                .orElseThrow(() -> new RuntimeException("타겟 방을 찾을 수 없습니다: " + targetRoomId));
            targetRoom.unlock();
            chatRoomRepository.save(targetRoom);
            log.info("타겟 방 잠금 해제 완료: roomId={}", targetRoomId);
            
            // 소스 방들 잠금 해제
            for (String sourceRoomId : sourceRoomIds) {
                ChatRoom sourceRoom = chatRoomRepository.findById(sourceRoomId)
                    .orElseThrow(() -> new RuntimeException("소스 방을 찾을 수 없습니다: " + sourceRoomId));
                sourceRoom.unlock();
                chatRoomRepository.save(sourceRoom);
                log.info("소스 방 잠금 해제 완료: roomId={}", sourceRoomId);
            }
            
            log.info("방 잠금 해제 완료: mergeId={}", mergeId);
        } catch (Exception e) {
            log.error("방 잠금 해제 실패: mergeId={}", mergeId, e);
            throw new RuntimeException("방 잠금 해제 실패", e);
        }
    }
} 