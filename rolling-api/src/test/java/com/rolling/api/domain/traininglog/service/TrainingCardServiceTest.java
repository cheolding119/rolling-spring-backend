package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.config.TrainingCardFeatureProperties;
import com.rolling.api.domain.traininglog.dto.TrainingCardDetailResponse;
import com.rolling.api.domain.traininglog.dto.TrainingCardListItemResponse;
import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import com.rolling.api.domain.traininglog.entity.TrainingCardFavorite;
import com.rolling.api.domain.traininglog.entity.TrainingCardLike;
import com.rolling.api.domain.traininglog.repository.TrainingCardFavoriteRepository;
import com.rolling.api.domain.traininglog.repository.TrainingCardLikeRepository;
import com.rolling.api.domain.traininglog.repository.TrainingCardRepository;
import com.rolling.api.domain.traininglog.repository.TrainingLogCountProjection;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TrainingCardServiceTest {

    @Mock
    private TrainingCardRepository trainingCardRepository;

    @Mock
    private TrainingCardLikeRepository trainingCardLikeRepository;

    @Mock
    private TrainingCardFavoriteRepository trainingCardFavoriteRepository;

    @Mock
    private UserRepository userRepository;

    private TrainingCardService trainingCardService;

    @BeforeEach
    void setUp() {
        trainingCardService = new TrainingCardService(
                new TrainingCardFeatureProperties(true),
                trainingCardRepository,
                trainingCardLikeRepository,
                trainingCardFavoriteRepository,
                userRepository
        );
    }

    @Test
    @DisplayName("findCards normalizes blank query to null and applies filter arguments")
    void findCards_normalizesBlankQuery() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingCardRepository.searchActiveCards(null, TrainingCardLevel.BEGINNER, TrainingCardPosition.GUARD))
                .willReturn(List.of(trainingCard(
                        1L,
                        "Knee Cut Pass",
                        TrainingCardLevel.BEGINNER,
                        TrainingCardPosition.GUARD
                )));
        given(trainingCardLikeRepository.countByCardIds(List.of(1L))).willReturn(List.of(countProjection(1L, 3L)));
        given(trainingCardLikeRepository.findLikedCardIdsByUserIdAndCardIds(10L, List.of(1L))).willReturn(List.of(1L));
        given(trainingCardFavoriteRepository.findFavoritedCardIdsByUserIdAndCardIds(10L, List.of(1L))).willReturn(List.of());

        List<TrainingCardListItemResponse> response = trainingCardService.findCards(
                10L,
                "   ",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD
        );

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getTitle()).isEqualTo("Knee Cut Pass");
        assertThat(response.get(0).getLevel()).isEqualTo(TrainingCardLevel.BEGINNER);
        assertThat(response.get(0).getPosition()).isEqualTo(TrainingCardPosition.GUARD);
        assertThat(response.get(0).getLikeCount()).isEqualTo(3L);
        assertThat(response.get(0).getLikedByMe()).isTrue();
        assertThat(response.get(0).getFavoritedByMe()).isFalse();
    }

    @Test
    @DisplayName("findCards lowercases and trims search query")
    void findCards_normalizesSearchQuery() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingCardRepository.searchActiveCards("knee cut", null, null))
                .willReturn(List.of(trainingCard(
                        1L,
                        "Knee Cut Pass",
                        TrainingCardLevel.BEGINNER,
                        TrainingCardPosition.GUARD
                )));
        given(trainingCardLikeRepository.countByCardIds(List.of(1L))).willReturn(List.of());
        given(trainingCardLikeRepository.findLikedCardIdsByUserIdAndCardIds(10L, List.of(1L))).willReturn(List.of());
        given(trainingCardFavoriteRepository.findFavoritedCardIdsByUserIdAndCardIds(10L, List.of(1L))).willReturn(List.of(1L));

        List<TrainingCardListItemResponse> response = trainingCardService.findCards(10L, "  Knee Cut  ", null, null);

        assertThat(response).extracting(TrainingCardListItemResponse::getTitle)
                .containsExactly("Knee Cut Pass");
    }

    @Test
    @DisplayName("findCardDetail returns full response for active cards")
    void findCardDetail_returnsDetail() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingCardRepository.findByIdAndActiveTrue(1L))
                .willReturn(Optional.of(trainingCard(
                        1L,
                        "Knee Cut Pass",
                        TrainingCardLevel.BEGINNER,
                        TrainingCardPosition.GUARD
                )));
        given(trainingCardLikeRepository.countByCardIds(List.of(1L))).willReturn(List.of(countProjection(1L, 2L)));
        given(trainingCardLikeRepository.existsByCard_IdAndUser_Id(1L, 10L)).willReturn(true);
        given(trainingCardFavoriteRepository.existsByCard_IdAndUser_Id(1L, 10L)).willReturn(true);

        TrainingCardDetailResponse response = trainingCardService.findCardDetail(10L, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Knee Cut Pass");
        assertThat(response.getYoutubeUrl()).isEqualTo("https://www.youtube.com/watch?v=kneecut");
        assertThat(response.getLikeCount()).isEqualTo(2L);
        assertThat(response.getLikedByMe()).isTrue();
        assertThat(response.getFavoritedByMe()).isTrue();
    }

    @Test
    @DisplayName("findCardDetail rejects missing cards")
    void findCardDetail_missingCard_throwsNotFound() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingCardRepository.findByIdAndActiveTrue(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> trainingCardService.findCardDetail(10L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("훈련카드를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("likeCard ignores duplicate likes")
    void likeCard_whenAlreadyLiked_doesNotSaveDuplicate() {
        User user = createUser(10L);
        TrainingCard card = trainingCard(1L, "Knee Cut Pass", TrainingCardLevel.BEGINNER, TrainingCardPosition.GUARD);
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));
        given(trainingCardRepository.findByIdAndActiveTrue(1L)).willReturn(Optional.of(card));
        given(trainingCardLikeRepository.existsByCard_IdAndUser_Id(1L, 10L)).willReturn(true);

        trainingCardService.likeCard(10L, 1L);

        verify(trainingCardLikeRepository, never()).save(any(TrainingCardLike.class));
    }

    @Test
    @DisplayName("favoriteCard creates favorite for active card")
    void favoriteCard_savesFavorite() {
        User user = createUser(10L);
        TrainingCard card = trainingCard(1L, "Knee Cut Pass", TrainingCardLevel.BEGINNER, TrainingCardPosition.GUARD);
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(user));
        given(trainingCardRepository.findByIdAndActiveTrue(1L)).willReturn(Optional.of(card));
        given(trainingCardFavoriteRepository.existsByCard_IdAndUser_Id(1L, 10L)).willReturn(false);

        trainingCardService.favoriteCard(10L, 1L);

        verify(trainingCardFavoriteRepository).save(any(TrainingCardFavorite.class));
    }

    @Test
    @DisplayName("unfavoriteCard ignores missing favorite after validating card")
    void unfavoriteCard_whenMissingFavorite_doesNothing() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingCardRepository.findByIdAndActiveTrue(1L))
                .willReturn(Optional.of(trainingCard(1L, "Knee Cut Pass", TrainingCardLevel.BEGINNER, TrainingCardPosition.GUARD)));
        given(trainingCardFavoriteRepository.findByCard_IdAndUser_Id(1L, 10L)).willReturn(Optional.empty());

        trainingCardService.unfavoriteCard(10L, 1L);

        verify(trainingCardFavoriteRepository, never()).delete(any(TrainingCardFavorite.class));
    }

    @Test
    @DisplayName("findCards rejects requests when feature is disabled")
    void findCards_whenFeatureDisabled_throwsNotFound() {
        trainingCardService = new TrainingCardService(
                new TrainingCardFeatureProperties(false),
                trainingCardRepository,
                trainingCardLikeRepository,
                trainingCardFavoriteRepository,
                userRepository
        );

        assertThatThrownBy(() -> trainingCardService.findCards(10L, null, null, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("훈련카드 기능이 비활성화되었습니다");
    }

    private TrainingCard trainingCard(
            Long id,
            String title,
            TrainingCardLevel level,
            TrainingCardPosition position
    ) {
        TrainingCard card = TrainingCard.builder()
                .title(title)
                .summary("상대 가드를 가로질러 압박으로 통과하는 패스")
                .topic("PASS")
                .level(level)
                .position(position)
                .situationSummary("니쉴드와 하프가드 압박 상황")
                .description("무릎을 상대 다리 사이로 넣고 상체 압박으로 통과한다.")
                .situationDescription("상대가 무릎 방패를 세우고 거리를 만들려고 할 때 사용한다.")
                .startingPositionDescription("탑에서 한쪽 무릎이 상대 다리 라인을 넘볼 수 있는 거리")
                .flowDescription("무릎 진입 -> 상체 압박 -> 반대 다리 정리 -> 사이드 고정")
                .keyPoints("무릎 각도와 머리 위치를 잃지 않는다.")
                .commonMistakes("상체 압박이 빠져 무릎만 먼저 들어간다.")
                .cautions("상대 언더훅을 허용하지 않도록 주의한다.")
                .youtubeUrl("https://www.youtube.com/watch?v=kneecut")
                .active(true)
                .displayOrder(0)
                .build();
        ReflectionTestUtils.setField(card, "id", id);
        return card;
    }

    private User createUser(Long id) {
        User user = User.builder()
                .socialId("user-" + id)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("user-" + id)
                .email("user-" + id + "@test.com")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TrainingLogCountProjection countProjection(Long cardId, Long count) {
        return new TrainingLogCountProjection() {
            @Override
            public Long getEntryId() {
                return cardId;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
