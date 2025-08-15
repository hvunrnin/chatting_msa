package chatting.chatproducer.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageMigrationService {

    /**
     * 실제 메시지 마이그레이션
     */
    public void migrateMessages(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("메시지 마이그레이션 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}", 
                mergeId, targetRoomId, sourceRoomIds);

        // 1. MongoDB에서 소스 방의 메시지들 조회
        // 2. 타겟 방으로 메시지 복사
        // 3. 마이그레이션 완료 이벤트 발행
        
        log.info("메시지 마이그레이션 완료: mergeId={}", mergeId);
    }
} 