package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.MessageMigrationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageMigrationLogRepository extends JpaRepository<MessageMigrationLog, Long> {
    List<MessageMigrationLog> findByMergeIdAndStatus(
        String mergeId, MessageMigrationLog.MigrationStatus status, Pageable pageable);
} 