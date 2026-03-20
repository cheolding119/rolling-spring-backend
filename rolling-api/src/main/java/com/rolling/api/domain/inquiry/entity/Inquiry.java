package com.rolling.api.domain.inquiry.entity;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "inquiries",
        indexes = {
                @Index(name = "idx_inquiries_user_created_at", columnList = "user_id,created_at"),
                @Index(name = "idx_inquiries_status_created_at", columnList = "status,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private InquiryStatus status;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @Column(name = "answered_by_user_id")
    private Long answeredByUserId;

    @Column(name = "answered_at")
    private LocalDateTime answeredAt;

    @Builder
    public Inquiry(User user,
                   String title,
                   String content,
                   InquiryStatus status,
                   String answerContent,
                   Long answeredByUserId,
                   LocalDateTime answeredAt) {
        this.user = user;
        this.title = title;
        this.content = content;
        this.status = status;
        this.answerContent = answerContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredAt = answeredAt;
    }

    public void answer(String answerContent, Long answeredByUserId, LocalDateTime answeredAt) {
        this.answerContent = answerContent;
        this.answeredByUserId = answeredByUserId;
        this.answeredAt = answeredAt;
        this.status = InquiryStatus.ANSWERED;
    }

    public void updateStatus(InquiryStatus status) {
        this.status = status;
    }

    public boolean hasAnswer() {
        return StringUtils.hasText(answerContent);
    }
}
