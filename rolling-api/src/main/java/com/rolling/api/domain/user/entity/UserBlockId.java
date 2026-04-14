package com.rolling.api.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserBlockId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "blocked_user_id")
    private Long blockedUserId;
}
