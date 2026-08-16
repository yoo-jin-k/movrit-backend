package com.movrit.movrit.creator.service;

import com.movrit.movrit.creator.dto.CreatorDtos;
import com.movrit.movrit.creator.entity.Creator;
import com.movrit.movrit.creator.repository.CreatorRepository;
import com.movrit.movrit.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorService {
    private final CreatorRepository creatorRepository;
    private final MemberService memberService;

    @Transactional
    public CreatorDtos.Response create(CreatorDtos.CreateRequest request) {
        Creator creator = Creator.builder()
                .member(memberService.getMember(request.memberId()))
                .nickname(request.nickname().trim())
                .bio(request.bio())
                .primaryCategory(request.primaryCategory())
                .status(request.status().trim())
                .build();
        return CreatorDtos.Response.from(creatorRepository.save(creator));
    }

    public List<CreatorDtos.Response> findAll() {
        return creatorRepository.findAll().stream().map(CreatorDtos.Response::from).toList();
    }

    public CreatorDtos.Response findById(Long id) {
        return CreatorDtos.Response.from(getCreator(id));
    }

    @Transactional
    public CreatorDtos.Response update(Long id, CreatorDtos.UpdateRequest request) {
        Creator creator = getCreator(id);
        creator.update(request.nickname().trim(), request.bio(), request.primaryCategory(), request.status().trim());
        return CreatorDtos.Response.from(creator);
    }

    @Transactional
    public void delete(Long id) {
        creatorRepository.delete(getCreator(id));
    }

    private Creator getCreator(Long id) {
        return creatorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "인플루언서를 찾을 수 없어."));
    }
}
