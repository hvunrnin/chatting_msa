package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.kafka.dto.MergeEventDTO;
import chatting.chatproducer.kafka.producer.MergeEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageMigrationService {

    private final MongoTemplate mongoTemplate;
    private final MergeEventProducer mergeEventProducer;

    /**
     * 메시지 마이그레이션
     */
    @Transactional
    public void migrateMessages(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("메시지 마이그레이션 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}", 
                mergeId, targetRoomId, sourceRoomIds);

        try {
            // 1. 소스 방들의 메시지 개수 조회
            int totalMigratedMessages = 0;
            for (String sourceRoomId : sourceRoomIds) {
                Query countQuery = new Query(Criteria.where("roomId").is(sourceRoomId));
                long messageCount = mongoTemplate.count(countQuery, "chat_messages_ind");
                totalMigratedMessages += messageCount;
                
                log.info("소스 방 {} 메시지 개수: {}", sourceRoomId, messageCount);
            }

            // 2. 소스 방들의 메시지를 타겟 방으로 일괄 변경
            Query updateQuery = new Query(Criteria.where("roomId").in(sourceRoomIds));
            Update update = new Update().set("roomId", targetRoomId);
            
            var result = mongoTemplate.updateMulti(updateQuery, update, "chat_messages_ind");
            
            log.info("메시지 마이그레이션 완료: mergeId={}, updatedCount={}, totalMessages={}", 
                    mergeId, result.getModifiedCount(), totalMigratedMessages);

        } catch (Exception e) {
            log.error("메시지 마이그레이션 실패: mergeId={}, targetRoomId={}", mergeId, targetRoomId, e);
            throw new RuntimeException("메시지 마이그레이션 실패", e);
        }
    }

    /**
     * 마이그레이션 전 검증
     */
    public void validateMigration(String targetRoomId, List<String> sourceRoomIds) {
        log.info("메시지 마이그레이션 검증: targetRoomId={}, sourceRoomIds={}", targetRoomId, sourceRoomIds);

        // 1. 타겟 방이 존재하는지 확인
        Query targetQuery = new Query(Criteria.where("roomId").is(targetRoomId));
        long targetMessageCount = mongoTemplate.count(targetQuery, "chat_messages_ind");
        log.info("타겟 방 {} 기존 메시지 개수: {}", targetRoomId, targetMessageCount);

        // 2. 소스 방들이 존재하는지 확인
        for (String sourceRoomId : sourceRoomIds) {
            Query sourceQuery = new Query(Criteria.where("roomId").is(sourceRoomId));
            long sourceMessageCount = mongoTemplate.count(sourceQuery, "chat_messages_ind");
            log.info("소스 방 {} 메시지 개수: {}", sourceRoomId, sourceMessageCount);
            
            if (sourceMessageCount == 0) {
                log.warn("소스 방 {}에 메시지가 없습니다.", sourceRoomId);
            }
        }
    }
} 