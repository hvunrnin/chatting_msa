package chatting.chatproducer.domain.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String userId;

    private LocalDateTime createdAt;

    public static User of(String userId) {
        return User.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
