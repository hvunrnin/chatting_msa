package chatting.chatproducer.domain.room.service;

import chatting.chatproducer.domain.chatmessage.document.ChatMessageDocument;
import chatting.chatproducer.domain.chatmessage.repository.ChatMessageMongoRepository;
import chatting.chatproducer.domain.room.entity.MessageMigrationLog;
import chatting.chatproducer.domain.room.repository.MessageMigrationLogRepository;
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
    private final ChatMessageMongoRepository chatMessageMongoRepository;
    private final MessageMigrationLogRepository messageMigrationLogRepository;

    /**
     * 메시지 마이그레이션 (업데이트 방식)
     */
    @Transactional
    public void migrateMessages(String mergeId, String targetRoomId, List<String> sourceRoomIds) {
        log.info("메시지 마이그레이션 시작: mergeId={}, targetRoomId={}, sourceRoomIds={}",
                mergeId, targetRoomId, sourceRoomIds);

        int totalMigrated = 0;

        for (String sourceRoomId : sourceRoomIds) {
            // roomId == sourceRoomId 인 문서들을 페이징으로 조회
            int page = 0;
            while (true) {
                List<ChatMessageDocument> batch = 
                    chatMessageMongoRepository.findByRoomIdOrderByTimestampAsc(sourceRoomId);
                if (batch.isEmpty()) break;

                for (ChatMessageDocument message : batch) {
                    try {
                        // 현재 위치 확인
                        boolean alreadyInTarget = targetRoomId.equals(message.getRoomId());

                        // 로그 먼저 기록
                        MessageMigrationLog migrationLog = MessageMigrationLog.of(
                            mergeId, sourceRoomId, targetRoomId,
                            message.getId(), alreadyInTarget, message.getRoomId()
                        );
                        messageMigrationLogRepository.save(migrationLog);

                        if (!alreadyInTarget) {
                            // 이동: roomId를 target으로 업데이트
                            Query query = new Query(Criteria.where("_id").is(message.getId())
                                                          .and("roomId").is(sourceRoomId));
                            Update update = new Update().set("roomId", targetRoomId);

                            var result = mongoTemplate.updateFirst(query, update, ChatMessageDocument.class);
                            if (result.getModifiedCount() > 0) {
                                totalMigrated++;
                                log.debug("메시지 이동: messageId={}, {} -> {}", 
                                        message.getId(), sourceRoomId, targetRoomId);
                            } else {
                                // 멱등/경합: 이미 이동됐거나 조건 미일치
                                log.debug("이동 스킵/멱등: messageId={}, from={}", message.getId(), sourceRoomId);
                            }
                        } else {
                            // 이미 타겟에 있음 → skip
                            log.debug("이미 타겟에 존재: messageId={}, targetRoomId={}", 
                                    message.getId(), targetRoomId);
                        }

                    } catch (Exception e) {
                        log.error("개별 메시지 마이그레이션 실패: mergeId={}, messageId={}, sourceRoomId={}",
                                  mergeId, message.getId(), sourceRoomId, e);
                    }
                }
                
                page++;
                if (batch.size() < 1000) break; // 마지막 페이지
            }
        }
        
        log.info("메시지 마이그레이션 완료: mergeId={}, totalMigrated={}", mergeId, totalMigrated);
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