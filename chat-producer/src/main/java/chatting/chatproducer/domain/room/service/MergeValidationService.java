package chatting.chatproducer.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MergeValidationService {

    /**
     * 병합 검증
     */
    public void validateMerge(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("병합 검증 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}", 
                mergeId, targetRoomId, sourceRoomIds);

        // 1. 메시지 마이그레이션 검증
        // 2. 사용자 마이그레이션 검증
        // 3. 데이터 일관성 검증
        
        log.info("병합 검증 완료: mergeId={}", mergeId);
    }
} 