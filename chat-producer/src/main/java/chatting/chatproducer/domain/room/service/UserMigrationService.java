package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.room.entity.RoomUser;
import chatting.chatproducer.domain.room.entity.UserMigrationLog;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
import chatting.chatproducer.domain.room.repository.UserMigrationLogRepository;
import chatting.chatproducer.kafka.dto.MergeEventDTO;
import chatting.chatproducer.kafka.producer.MergeEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMigrationService {

    private final RoomUserRepository roomUserRepository;
    private final UserMigrationLogRepository userMigrationLogRepository;
    private final MergeEventProducer mergeEventProducer;

    /**
     * 사용자 마이그레이션
     */
    @Transactional
    public void migrateUsers(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("사용자 마이그레이션 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}", 
                mergeId, targetRoomId, sourceRoomIds);

        try {
            // 1. 소스 방들의 사용자들 조회
            List<RoomUser> sourceRoomUsers = roomUserRepository.findByRoomIdIn(sourceRoomIds);
            log.info("소스 방 사용자 조회 완료: mergeId={}, userCount={}", mergeId, sourceRoomUsers.size());

            // 2. 타겟 방의 기존 사용자들 조회
            List<RoomUser> targetRoomUsers = roomUserRepository.findByRoomId(targetRoomId);
            Set<String> existingUserIds = targetRoomUsers.stream()
                    .map(RoomUser::getUserId)
                    .collect(Collectors.toSet());
            
            log.info("타겟 방 기존 사용자 조회 완료: mergeId={}, existingUserCount={}", mergeId, existingUserIds.size());

            // 3. 모든 소스 방 사용자에 대해 로그 기록 및 마이그레이션 수행
            log.info("마이그레이션 대상 사용자 처리 시작: mergeId={}, totalUserCount={}", 
                    mergeId, sourceRoomUsers.size());

            // 4. 사용자들을 타겟 방으로 이동 (중복 여부와 관계없이 모든 사용자 처리)
            int migratedCount = 0;
            for (RoomUser sourceUser : sourceRoomUsers) {
                try {
                    // 이전 상태 스냅샷 생성
                    boolean wasMemberInTo = existingUserIds.contains(sourceUser.getUserId());
                    String prevRoleInTo = null;
                    if (wasMemberInTo) {
                        // 타겟 방에 이미 있는 사용자의 경우 현재 역할 조회
                        RoomUser existingUser = targetRoomUsers.stream()
                                .filter(user -> user.getUserId().equals(sourceUser.getUserId()))
                                .findFirst()
                                .orElse(null);
                        prevRoleInTo = existingUser != null ? existingUser.getRole().name() : null;
                    }
                    
                    boolean wasMemberInFrom = true; // 소스 방에서 마이그레이션하는 사용자이므로 true
                    String prevRoleInFrom = sourceUser.getRole().name();

                    // 마이그레이션 로그 먼저 기록 (롤백을 위한 스냅샷)
                    UserMigrationLog migrationLog = UserMigrationLog.of(
                            mergeId, 
                            sourceUser.getRoomId(), 
                            targetRoomId, 
                            sourceUser.getUserId(),
                            wasMemberInFrom,
                            wasMemberInTo,
                            prevRoleInFrom,
                            prevRoleInTo
                    );
                    userMigrationLogRepository.save(migrationLog);

                    // 중복 여부에 따라 다른 처리
                    if (!wasMemberInTo) {
                        // 타겟 방에 없는 사용자: 새로 추가
                        RoomUser targetUser = RoomUser.builder()
                                .roomId(targetRoomId)
                                .userId(sourceUser.getUserId())
                                .joinedAt(LocalDateTime.now()) // 현재 시간으로 업데이트
                                .role(sourceUser.getRole())
                                .build();
                        roomUserRepository.save(targetUser);
                        migratedCount++;
                        log.debug("새 사용자 추가: userId={}, targetRoomId={}", sourceUser.getUserId(), targetRoomId);
                    } else {
                        // 타겟 방에 이미 있는 사용자: 역할 업데이트 (필요한 경우)
                        if (prevRoleInTo != null && !prevRoleInTo.equals(sourceUser.getRole().name())) {
                            roomUserRepository.updateRole(sourceUser.getUserId(), targetRoomId, sourceUser.getRole().name());
                            log.debug("기존 사용자 역할 업데이트: userId={}, targetRoomId={}, newRole={}", 
                                    sourceUser.getUserId(), targetRoomId, sourceUser.getRole().name());
                        }
                    }

                    log.debug("사용자 마이그레이션 완료: mergeId={}, userId={}, sourceRoomId={}, targetRoomId={}, wasMemberInTo={}", 
                            mergeId, sourceUser.getUserId(), sourceUser.getRoomId(), targetRoomId, wasMemberInTo);

                } catch (Exception e) {
                    log.error("개별 사용자 마이그레이션 실패: mergeId={}, userId={}, sourceRoomId={}", 
                            mergeId, sourceUser.getUserId(), sourceUser.getRoomId(), e);
                    // 개별 실패는 로그만 남기고 계속 진행
                }
            }

            // 5. 소스 방의 모든 사용자들 삭제 (중복 여부와 관계없이)
            // 병합 후 소스 방이 삭제되므로 모든 사용자 정보도 함께 삭제
            for (String sourceRoomId : sourceRoomIds) {
                try {
                    List<RoomUser> usersToDelete = roomUserRepository.findByRoomId(sourceRoomId);
                    log.info("삭제할 사용자 조회: sourceRoomId={}, count={}", sourceRoomId, usersToDelete.size());
                    
                    if (!usersToDelete.isEmpty()) {
                        roomUserRepository.deleteAll(usersToDelete);
                        log.info("소스 방 사용자 삭제 성공: sourceRoomId={}, deletedCount={}", sourceRoomId, usersToDelete.size());
                    } else {
                        log.info("삭제할 사용자가 없음: sourceRoomId={}", sourceRoomId);
                    }
                } catch (Exception e) {
                    log.error("소스 방 사용자 삭제 실패: sourceRoomId={}", sourceRoomId, e);
                    throw new RuntimeException("소스 방 사용자 삭제 실패: " + sourceRoomId, e);
                }
            }

            log.info("사용자 마이그레이션 완료: mergeId={}, migratedCount={}, totalSourceUsers={}", 
                    mergeId, migratedCount, sourceRoomUsers.size());

        } catch (Exception e) {
            log.error("사용자 마이그레이션 실패: mergeId={}, targetRoomId={}", mergeId, targetRoomId, e);
            throw new RuntimeException("사용자 마이그레이션 실패", e);
        }
    }

    /**
     * 마이그레이션 전 검증
     */
    public void validateMigration(String targetRoomId, List<String> sourceRoomIds) {
        log.info("사용자 마이그레이션 검증: targetRoomId={}, sourceRoomIds={}", targetRoomId, sourceRoomIds);

        // 1. 타겟 방 기존 사용자 확인
        List<RoomUser> targetUsers = roomUserRepository.findByRoomId(targetRoomId);
        log.info("타겟 방 {} 기존 사용자 개수: {}", targetRoomId, targetUsers.size());

        // 2. 소스 방들 사용자 확인
        for (String sourceRoomId : sourceRoomIds) {
            List<RoomUser> sourceUsers = roomUserRepository.findByRoomId(sourceRoomId);
            log.info("소스 방 {} 사용자 개수: {}", sourceRoomId, sourceUsers.size());
            
            if (sourceUsers.isEmpty()) {
                log.warn("소스 방 {}에 사용자가 없습니다.", sourceRoomId);
            }
        }

        // 3. 중복 사용자 확인
        Set<String> targetUserIds = targetUsers.stream()
                .map(RoomUser::getUserId)
                .collect(Collectors.toSet());

        for (String sourceRoomId : sourceRoomIds) {
            List<RoomUser> sourceUsers = roomUserRepository.findByRoomId(sourceRoomId);
            long duplicateCount = sourceUsers.stream()
                    .map(RoomUser::getUserId)
                    .filter(targetUserIds::contains)
                    .count();
            
            if (duplicateCount > 0) {
                log.info("소스 방 {}에서 중복 사용자 {}명", sourceRoomId, duplicateCount);
            }
        }
    }
} 