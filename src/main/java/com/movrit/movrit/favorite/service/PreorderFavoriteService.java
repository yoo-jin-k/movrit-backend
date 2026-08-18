package com.movrit.movrit.favorite.service;

import com.movrit.movrit.favorite.dto.FavoriteDtos;
import com.movrit.movrit.favorite.entity.PreorderFavorite;
import com.movrit.movrit.favorite.repository.PreorderFavoriteRepository;
import com.movrit.movrit.member.entity.Member;
import com.movrit.movrit.member.repository.MemberRepository;
import com.movrit.movrit.preorder.entity.Preorder;
import com.movrit.movrit.preorder.repository.PreorderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PreorderFavoriteService {

    private final PreorderFavoriteRepository favoriteRepository;
    private final MemberRepository memberRepository;
    private final PreorderRepository preorderRepository;

    @Transactional
    public FavoriteDtos.Response add(Long memberId, Long preorderId) {
        if (favoriteRepository.existsByMemberIdAndPreorderId(memberId, preorderId)) {
            return favoriteRepository.findByMemberIdAndPreorderId(memberId, preorderId)
                    .map(this::toResponse)
                    .orElseThrow();
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없어."));
        Preorder preorder = preorderRepository.findById(preorderId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "프리오더를 찾을 수 없어."));
        PreorderFavorite saved = favoriteRepository.save(PreorderFavorite.builder().member(member).preorder(preorder).build());
        return toResponse(saved);
    }

    @Transactional
    public void remove(Long memberId, Long preorderId) {
        favoriteRepository.findByMemberIdAndPreorderId(memberId, preorderId).ifPresent(favoriteRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<FavoriteDtos.Response> findMine(Long memberId) {
        return favoriteRepository.findAllByMemberIdOrderByIdDesc(memberId).stream()
                .filter(favorite -> favorite.getPreorder().getDeletedAt() == null)
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public FavoriteDtos.StatusResponse status(Long memberId, Long preorderId) {
        return new FavoriteDtos.StatusResponse(favoriteRepository.existsByMemberIdAndPreorderId(memberId, preorderId));
    }

    private FavoriteDtos.Response toResponse(PreorderFavorite favorite) {
        Preorder preorder = favorite.getPreorder();
        return new FavoriteDtos.Response(favorite.getId(), preorder.getId(), preorder.getTitle(), preorder.getPreorderPrice(), preorder.getCurrency(), preorder.getTargetQuantity(), preorder.getOrderedQuantity(), preorder.getStartAt(), preorder.getEndAt(), preorder.getStatus());
    }
}
