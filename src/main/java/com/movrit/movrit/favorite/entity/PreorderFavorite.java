package com.movrit.movrit.favorite.entity;

import com.movrit.movrit.common.entity.BaseEntity;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.preorder.entity.Preorder;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "preorder_favorite",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_preorder_favorite_member_preorder",
                columnNames = {"member_id", "preorder_id"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PreorderFavorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "preorder_id", nullable = false)
    private Preorder preorder;
}
