package com.rolling.api.domain.tournament.util;

import com.rolling.api.domain.openmat.entity.Region;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TournamentRegionResolverTest {

    @Test
    void resolvesRegionFromTournamentAddress() {
        assertThat(TournamentRegionResolver.resolve("서울 서초구 양재대로")).isEqualTo(Region.SEOUL);
        assertThat(TournamentRegionResolver.resolve("경기 광주시 체육관")).isEqualTo(Region.GYEONGGI);
        assertThat(TournamentRegionResolver.resolve("강원특별자치도 횡성군 체육관")).isEqualTo(Region.GANGWON);
        assertThat(TournamentRegionResolver.resolve("전북특별자치도 전주시 체육관")).isEqualTo(Region.JEONBUK);
        assertThat(TournamentRegionResolver.resolve("전남광주통합특별시 북구 북문대로")).isEqualTo(Region.GWANGJU);
        assertThat(TournamentRegionResolver.resolve("전남광주통합특별시 순천시 팔마로")).isEqualTo(Region.JEONNAM);
        assertThat(TournamentRegionResolver.resolve("안산시 체육관 (경기도 안산시)")).isEqualTo(Region.GYEONGGI);
        assertThat(TournamentRegionResolver.resolve("장소 미정")).isNull();
    }
}
