package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.room.entity.ChatRoom;
import chatting.chatproducer.domain.room.entity.RoomUser;
import chatting.chatproducer.domain.room.repository.ChatRoomRepository;
import chatting.chatproducer.domain.room.repository.RoomUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MergeValidationService {

    private final ChatRoomRepository chatRoomRepository;
    private final RoomUserRepository roomUserRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * 병합 검증
     */
    public void validateMerge(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("병합 검증 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}", 
                mergeId, targetRoomId, sourceRoomIds);

        try {
            // 1. 채팅방 존재 여부 검증
            validateRoomExistence(targetRoomId, sourceRoomIds);

            // 2. 메시지 마이그레이션 검증
            validateMessageMigration(targetRoomId, sourceRoomIds);

            // 3. 사용자 마이그레이션 검증
            validateUserMigration(targetRoomId, sourceRoomIds);

            // 4. 데이터 일관성 검증
            validateDataConsistency(targetRoomId, sourceRoomIds);

            log.info("병합 검증 완료: mergeId={}", mergeId);

        } catch (Exception e) {
            log.error("병합 검증 실패: mergeId={}, targetRoomId={}", mergeId, targetRoomId, e);
            throw new RuntimeException("병합 검증 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 채팅방 존재 여부 검증
     */
    private void validateRoomExistence(String targetRoomId, List<String> sourceRoomIds) {
        log.info("채팅방 존재 여부 검증 시작");

        // 타겟 방 존재 확인
        ChatRoom targetRoom = chatRoomRepository.findById(targetRoomId)
                .orElseThrow(() -> new RuntimeException("타겟 방이 존재하지 않습니다: " + targetRoomId));
        
        log.info("타겟 방 확인 완료: roomId={}, name={}, status={}", 
                targetRoom.getRoomId(), targetRoom.getName(), targetRoom.getStatus());

        // 소스 방들 존재 확인
        for (String sourceRoomId : sourceRoomIds) {
            ChatRoom sourceRoom = chatRoomRepository.findById(sourceRoomId)
                    .orElseThrow(() -> new RuntimeException("소스 방이 존재하지 않습니다: " + sourceRoomId));
            
            log.info("소스 방 확인 완료: roomId={}, name={}, status={}", 
                    sourceRoom.getRoomId(), sourceRoom.getName(), sourceRoom.getStatus());
        }
    }

    /**
     * 메시지 마이그레이션 검증
     */
    private void validateMessageMigration(String targetRoomId, List<String> sourceRoomIds) {
        log.info("메시지 마이그레이션 검증 시작");

        // 타겟 방의 메시지 개수 확인
        Query targetQuery = new Query(Criteria.where("roomId").is(targetRoomId));
        long targetMessageCount = mongoTemplate.count(targetQuery, "chat_messages_ind");
        log.info("타겟 방 메시지 개수: {}", targetMessageCount);

        // 소스 방들의 메시지가 타겟 방으로 이동되었는지 확인
        for (String sourceRoomId : sourceRoomIds) {
            Query sourceQuery = new Query(Criteria.where("roomId").is(sourceRoomId));
            long sourceMessageCount = mongoTemplate.count(sourceQuery, "chat_messages_ind");
            
            if (sourceMessageCount > 0) {
                throw new RuntimeException("소스 방 " + sourceRoomId + "에 아직 메시지가 남아있습니다: " + sourceMessageCount + "개");
            }
            
            log.info("소스 방 {} 메시지 마이그레이션 확인 완료", sourceRoomId);
        }
    }

    /**
     * 사용자 마이그레이션 검증
     */
    private void validateUserMigration(String targetRoomId, List<String> sourceRoomIds) {
        log.info("사용자 마이그레이션 검증 시작");

        // 타겟 방의 사용자 개수 확인
        List<RoomUser> targetUsers = roomUserRepository.findByRoomId(targetRoomId);
        log.info("타겟 방 사용자 개수: {}", targetUsers.size());

        // 소스 방들의 사용자가 타겟 방으로 이동되었는지 확인
        for (String sourceRoomId : sourceRoomIds) {
            List<RoomUser> sourceUsers = roomUserRepository.findByRoomId(sourceRoomId);
            
            if (!sourceUsers.isEmpty()) {
                throw new RuntimeException("소스 방 " + sourceRoomId + "에 아직 사용자가 남아있습니다: " + sourceUsers.size() + "명");
            }
            
            log.info("소스 방 {} 사용자 마이그레이션 확인 완료", sourceRoomId);
        }
    }

    /**
     * 데이터 일관성 검증
     */
    private void validateDataConsistency(String targetRoomId, List<String> sourceRoomIds) {
        log.info("데이터 일관성 검증 시작");

        // 1. 타겟 방 상태 확인
        ChatRoom targetRoom = chatRoomRepository.findById(targetRoomId)
                .orElseThrow(() -> new RuntimeException("타겟 방을 찾을 수 없습니다: " + targetRoomId));

        if (targetRoom.getStatus() != ChatRoom.RoomStatus.MERGING) {
            log.warn("타겟 방 상태가 MERGING이 아닙니다: {}", targetRoom.getStatus());
        }

        // 2. 소스 방 상태 확인
        for (String sourceRoomId : sourceRoomIds) {
            ChatRoom sourceRoom = chatRoomRepository.findById(sourceRoomId)
                    .orElseThrow(() -> new RuntimeException("소스 방을 찾을 수 없습니다: " + sourceRoomId));

            if (sourceRoom.getStatus() != ChatRoom.RoomStatus.MERGING) {
                log.warn("소스 방 {} 상태가 MERGING이 아닙니다: {}", sourceRoomId, sourceRoom.getStatus());
            }
        }

        // 3. 메시지와 사용자 데이터 일관성 확인
        long messageCount = mongoTemplate.count(new Query(Criteria.where("roomId").is(targetRoomId)), "chat_messages_ind");
        long userCount = roomUserRepository.countByRoomId(targetRoomId);

        log.info("데이터 일관성 확인 완료: 메시지 {}개, 사용자 {}명", messageCount, userCount);

        if (messageCount == 0 && userCount == 0) {
            log.warn("타겟 방에 메시지와 사용자가 모두 없습니다.. 병합이 제대로 되지 않았을 수 있습니다........");
        }
    }
} 