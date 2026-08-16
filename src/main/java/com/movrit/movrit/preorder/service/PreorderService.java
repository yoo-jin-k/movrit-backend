package com.movrit.movrit.preorder.service;

import com.movrit.movrit.preorder.repository.PreorderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreorderService {
    private final PreorderRepository preorderRepository;
}
