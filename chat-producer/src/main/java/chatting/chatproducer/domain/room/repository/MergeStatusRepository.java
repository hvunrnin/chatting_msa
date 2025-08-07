package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.MergeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MergeStatusRepository extends JpaRepository<MergeStatus, String> {
    
    // 특정 상태의 병합 작업들 조회
    List<MergeStatus> findByStatus(String status);
    
    // 특정 방과 관련된 병합 작업 조회
    @Query("SELECT m FROM MergeStatus m WHERE m.targetRoomId = :roomId OR :roomId MEMBER OF m.sourceRoomIds")
    List<MergeStatus> findByRoomId(@Param("roomId") String roomId);
    
    // 진행 중인 병합 작업 조회
    List<MergeStatus> findByStatusAndCurrentStepNot(String status, MergeStatus.MergeStep step);
    
    // 최근 병합 작업들 조회
    @Query("SELECT m FROM MergeStatus m ORDER BY m.startedAt DESC")
    List<MergeStatus> findRecentMerges();
} 