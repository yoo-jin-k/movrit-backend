package com.movrit.movrit.creator.service;

import com.movrit.movrit.creator.repository.CreatorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreatorService {
    private final CreatorRepository creatorRepository;
}
