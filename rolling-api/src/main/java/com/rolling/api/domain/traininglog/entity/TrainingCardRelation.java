package com.rolling.api.domain.traininglog.entity;

import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "training_card_relations",
        indexes = {
                @Index(name = "idx_training_card_relations_card_id", columnList = "card_id"),
                @Index(name = "idx_training_card_relations_related_card_id", columnList = "related_card_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingCardRelation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "card_id", nullable = false)
    private TrainingCard card;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "related_card_id", nullable = false)
    private TrainingCard relatedCard;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    public TrainingCardRelation(TrainingCard card, TrainingCard relatedCard, int displayOrder) {
        this.card = card;
        this.relatedCard = relatedCard;
        this.displayOrder = displayOrder;
    }
}
