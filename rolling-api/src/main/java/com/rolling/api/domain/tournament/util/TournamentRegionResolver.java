package com.rolling.api.domain.tournament.util;

import com.rolling.api.domain.openmat.entity.Region;

public final class TournamentRegionResolver {

    private TournamentRegionResolver() {
    }

    public static Region resolve(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String normalized = location.trim().replaceAll("\\s+", " ");
        String province = normalized.split(" ", 2)[0];
        if (province.equals("전남광주통합특별시")) {
            return isGwangjuDistrict(normalized) ? Region.GWANGJU : Region.JEONNAM;
        }

        Region fromPrefix = switch (province) {
            case "서울", "서울시", "서울특별시" -> Region.SEOUL;
            case "경기", "경기도" -> Region.GYEONGGI;
            case "인천", "인천광역시" -> Region.INCHEON;
            case "대전", "대전광역시" -> Region.DAEJEON;
            case "세종", "세종시", "세종특별자치시" -> Region.SEJONG;
            case "충북", "충청북도" -> Region.CHUNGBUK;
            case "충남", "충청남도" -> Region.CHUNGNAM;
            case "부산", "부산광역시" -> Region.BUSAN;
            case "대구", "대구광역시" -> Region.DAEGU;
            case "울산", "울산광역시" -> Region.ULSAN;
            case "경북", "경상북도" -> Region.GYEONGBUK;
            case "경남", "경상남도" -> Region.GYEONGNAM;
            case "광주", "광주광역시" -> Region.GWANGJU;
            case "전북", "전라북도", "전북특별자치도" -> Region.JEONBUK;
            case "전남", "전라남도" -> Region.JEONNAM;
            case "강원", "강원도", "강원특별자치도" -> Region.GANGWON;
            case "제주", "제주도", "제주특별자치도" -> Region.JEJU;
            default -> null;
        };
        if (fromPrefix != null) {
            return fromPrefix;
        }

        if (normalized.contains("서울특별시")) return Region.SEOUL;
        if (normalized.contains("경기도")) return Region.GYEONGGI;
        if (normalized.contains("인천광역시")) return Region.INCHEON;
        if (normalized.contains("대전광역시")) return Region.DAEJEON;
        if (normalized.contains("세종특별자치시")) return Region.SEJONG;
        if (normalized.contains("충청북도")) return Region.CHUNGBUK;
        if (normalized.contains("충청남도")) return Region.CHUNGNAM;
        if (normalized.contains("부산광역시")) return Region.BUSAN;
        if (normalized.contains("대구광역시")) return Region.DAEGU;
        if (normalized.contains("울산광역시")) return Region.ULSAN;
        if (normalized.contains("경상북도")) return Region.GYEONGBUK;
        if (normalized.contains("경상남도")) return Region.GYEONGNAM;
        if (normalized.contains("광주광역시")) return Region.GWANGJU;
        if (normalized.contains("전북특별자치도") || normalized.contains("전라북도")) return Region.JEONBUK;
        if (normalized.contains("전라남도")) return Region.JEONNAM;
        if (normalized.contains("강원특별자치도") || normalized.contains("강원도")) return Region.GANGWON;
        if (normalized.contains("제주특별자치도") || normalized.contains("제주도")) return Region.JEJU;
        return null;
    }

    private static boolean isGwangjuDistrict(String address) {
        return address.contains("광산구")
                || address.contains("동구")
                || address.contains("서구")
                || address.contains("남구")
                || address.contains("북구");
    }
}
