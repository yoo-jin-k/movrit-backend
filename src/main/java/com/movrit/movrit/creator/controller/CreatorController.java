package com.movrit.movrit.creator.controller;

import com.movrit.movrit.creator.service.CreatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/creators")
public class CreatorController {
    private final CreatorService creatorService;
}
