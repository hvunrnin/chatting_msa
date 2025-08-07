package chatting.chatproducer.domain.room.repository;

import chatting.chatproducer.domain.room.entity.RoomUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomUserRepository extends JpaRepository<RoomUser, RoomUser.RoomUserId> {
    List<RoomUser> findAllByUserId(String userId);
    
    // 특정 방의 모든 유저 조회
    List<RoomUser> findByRoomId(String roomId);
    
    // 여러 방의 유저들 조회
    List<RoomUser> findByRoomIdIn(List<String> roomIds);
    
    // 특정 유저가 속한 방들 조회
    @Query("SELECT ru.roomId FROM RoomUser ru WHERE ru.userId = :userId")
    List<String> findRoomIdsByUserId(@Param("userId") String userId);
    
    // 특정 방의 유저 수 조회
    @Query("SELECT COUNT(ru) FROM RoomUser ru WHERE ru.roomId = :roomId")
    long countByRoomId(@Param("roomId") String roomId);
}

