package com.movrit.movrit.preorder.controller;

import com.movrit.movrit.preorder.service.PreorderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/preorders")
public class PreorderController {
    private final PreorderService preorderService;
}
