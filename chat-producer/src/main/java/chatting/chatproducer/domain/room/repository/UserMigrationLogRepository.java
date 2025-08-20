package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.UserMigrationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserMigrationLogRepository extends JpaRepository<UserMigrationLog, Long> {

    /**
     * 특정 병합의 마이그레이션된 사용자 로그 조회
     */
    List<UserMigrationLog> findByMergeIdAndStatus(String mergeId, UserMigrationLog.MigrationStatus status);

    /**
     * 특정 병합의 모든 사용자 마이그레이션 로그 조회
     */
    List<UserMigrationLog> findByMergeId(String mergeId);

    /**
     * 특정 병합에서 롤백되지 않은 사용자 마이그레이션 로그 조회
     */
    @Query("SELECT u FROM UserMigrationLog u WHERE u.mergeId = :mergeId AND u.status = 'MIGRATED'")
    List<UserMigrationLog> findMigratedUsersByMergeId(@Param("mergeId") String mergeId);

    /**
     * 특정 사용자의 마이그레이션 로그 조회
     */
    List<UserMigrationLog> findByUserIdAndStatus(String userId, UserMigrationLog.MigrationStatus status);
    
    /**
     * 특정 병합의 마이그레이션된 사용자 로그 페이징 조회
     */
    List<UserMigrationLog> findByMergeIdAndStatus(String mergeId, UserMigrationLog.MigrationStatus status, Pageable pageable);
} 