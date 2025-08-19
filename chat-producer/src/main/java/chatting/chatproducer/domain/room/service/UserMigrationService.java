package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.room.entity.RoomUser;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
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

            // 3. 중복되지 않는 사용자들만 필터링
            List<RoomUser> usersToMigrate = sourceRoomUsers.stream()
                    .filter(user -> !existingUserIds.contains(user.getUserId()))
                    .collect(Collectors.toList());

            log.info("마이그레이션 대상 사용자 필터링 완료: mergeId={}, migrateUserCount={}", 
                    mergeId, usersToMigrate.size());

            // 4. 사용자들을 타겟 방으로 이동
            int migratedCount = 0;
            for (RoomUser sourceUser : usersToMigrate) {
                try {
                    // 새로운 RoomUser 생성 (타겟 방으로)
                    RoomUser targetUser = RoomUser.builder()
                            .roomId(targetRoomId)
                            .userId(sourceUser.getUserId())
                            .joinedAt(LocalDateTime.now()) // 현재 시간으로 업데이트
                            .role(sourceUser.getRole())
                            .build();

                    roomUserRepository.save(targetUser);
                    migratedCount++;

                    log.debug("사용자 마이그레이션 완료: mergeId={}, userId={}, sourceRoomId={}, targetRoomId={}", 
                            mergeId, sourceUser.getUserId(), sourceUser.getRoomId(), targetRoomId);

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

            // 6. 마이그레이션 완료 이벤트 발행
            MergeEventDTO event = MergeEventDTO.builder()
                    .mergeId(mergeId)
                    .targetRoomId(targetRoomId)
                    .sourceRoomIds(sourceRoomIds)
                    .migratedUserCount(migratedCount)
                    .build();

            mergeEventProducer.publishUsersMigrated(event);

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