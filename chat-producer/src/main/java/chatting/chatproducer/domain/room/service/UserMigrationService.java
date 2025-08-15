package chatting.chatproducer.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMigrationService {
    /**
     * 실제 사용자 마이그레이션
     */
    public void migrateUsers(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("사용자 마이그레이션 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}", 
                mergeId, targetRoomId, sourceRoomIds);

        // 1. 소스 방의 사용자들 조회
        // 2. 타겟 방으로 사용자 이동
        // 3. 마이그레이션 완료 이벤트 발행
        
        log.info("사용자 마이그레이션 완료: mergeId={}", mergeId);
    }
} 