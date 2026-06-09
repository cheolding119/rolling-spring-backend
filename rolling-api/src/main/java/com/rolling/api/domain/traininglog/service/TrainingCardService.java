package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.config.TrainingCardFeatureProperties;
import com.rolling.api.domain.traininglog.dto.TrainingCardDetailResponse;
import com.rolling.api.domain.traininglog.dto.TrainingCardListItemResponse;
import com.rolling.api.domain.traininglog.entity.TrainingCardFavorite;
import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardLike;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import com.rolling.api.domain.traininglog.repository.TrainingCardFavoriteRepository;
import com.rolling.api.domain.traininglog.repository.TrainingCardLikeRepository;
import com.rolling.api.domain.traininglog.repository.TrainingCardRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingCardService {

    private final TrainingCardFeatureProperties trainingCardFeatureProperties;
    private final TrainingCardRepository trainingCardRepository;
    private final TrainingCardLikeRepository trainingCardLikeRepository;
    private final TrainingCardFavoriteRepository trainingCardFavoriteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TrainingCardListItemResponse> findCards(
            Long userId,
            String query,
            TrainingCardLevel level,
            TrainingCardPosition position
    ) {
        requireFeatureEnabled();
        requireActiveUserExists(userId);
        String normalizedQuery = normalizeQuery(query);
        List<TrainingCard> cards = normalizedQuery == null
                ? trainingCardRepository.findAllActiveCards(level, position)
                : trainingCardRepository.searchActiveCards(toSearchPattern(normalizedQuery), level, position);
        if (cards.isEmpty()) {
            return List.of();
        }

        List<Long> cardIds = cards.stream()
                .map(TrainingCard::getId)
                .toList();
        Map<Long, Long> likeCounts = toCountMap(trainingCardLikeRepository.countByCardIds(cardIds));
        Set<Long> likedCardIds = new HashSet<>(trainingCardLikeRepository.findLikedCardIdsByUserIdAndCardIds(userId, cardIds));
        Set<Long> favoritedCardIds = new HashSet<>(trainingCardFavoriteRepository.findFavoritedCardIdsByUserIdAndCardIds(userId, cardIds));

        return cards
                .stream()
                .map(card -> TrainingCardListItemResponse.from(
                        card,
                        likeCounts.getOrDefault(card.getId(), 0L),
                        likedCardIds.contains(card.getId()),
                        favoritedCardIds.contains(card.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingCardDetailResponse findCardDetail(Long userId, Long cardId) {
        requireFeatureEnabled();
        requireActiveUserExists(userId);
        TrainingCard card = trainingCardRepository.findByIdAndActiveTrue(cardId)
                .orElseThrow(() -> BusinessException.notFound("훈련카드를 찾을 수 없습니다"));
        long likeCount = toCountMap(trainingCardLikeRepository.countByCardIds(List.of(cardId)))
                .getOrDefault(cardId, 0L);
        boolean likedByMe = trainingCardLikeRepository.existsByCard_IdAndUser_Id(cardId, userId);
        boolean favoritedByMe = trainingCardFavoriteRepository.existsByCard_IdAndUser_Id(cardId, userId);
        return TrainingCardDetailResponse.from(card, likeCount, likedByMe, favoritedByMe);
    }

    @Transactional
    public void likeCard(Long userId, Long cardId) {
        requireFeatureEnabled();
        User user = getActiveUser(userId);
        TrainingCard card = getActiveCard(cardId);
        if (trainingCardLikeRepository.existsByCard_IdAndUser_Id(cardId, userId)) {
            return;
        }

        trainingCardLikeRepository.save(TrainingCardLike.builder()
                .card(card)
                .user(user)
                .build());
    }

    @Transactional
    public void unlikeCard(Long userId, Long cardId) {
        requireFeatureEnabled();
        requireActiveUserExists(userId);
        getActiveCard(cardId);
        trainingCardLikeRepository.findByCard_IdAndUser_Id(cardId, userId)
                .ifPresent(trainingCardLikeRepository::delete);
    }

    @Transactional
    public void favoriteCard(Long userId, Long cardId) {
        requireFeatureEnabled();
        User user = getActiveUser(userId);
        TrainingCard card = getActiveCard(cardId);
        if (trainingCardFavoriteRepository.existsByCard_IdAndUser_Id(cardId, userId)) {
            return;
        }

        trainingCardFavoriteRepository.save(TrainingCardFavorite.builder()
                .card(card)
                .user(user)
                .build());
    }

    @Transactional
    public void unfavoriteCard(Long userId, Long cardId) {
        requireFeatureEnabled();
        requireActiveUserExists(userId);
        getActiveCard(cardId);
        trainingCardFavoriteRepository.findByCard_IdAndUser_Id(cardId, userId)
                .ifPresent(trainingCardFavoriteRepository::delete);
    }

    private void requireActiveUserExists(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private TrainingCard getActiveCard(Long cardId) {
        return trainingCardRepository.findByIdAndActiveTrue(cardId)
                .orElseThrow(() -> BusinessException.notFound("훈련카드를 찾을 수 없습니다"));
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return null;
        }
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private String toSearchPattern(String normalizedQuery) {
        return "%" + normalizedQuery + "%";
    }

    private void requireFeatureEnabled() {
        if (!trainingCardFeatureProperties.enabled()) {
            throw BusinessException.notFound("훈련카드 기능이 비활성화되었습니다");
        }
    }

    private Map<Long, Long> toCountMap(List<com.rolling.api.domain.traininglog.repository.TrainingLogCountProjection> counts) {
        return counts.stream()
                .collect(Collectors.toMap(
                        com.rolling.api.domain.traininglog.repository.TrainingLogCountProjection::getEntryId,
                        com.rolling.api.domain.traininglog.repository.TrainingLogCountProjection::getCount
                ));
    }
}
