package com.movrit.movrit.shipping.controller;

import com.movrit.movrit.shipping.service.ShippingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shippings")
public class ShippingController {
    private final ShippingService shippingService;
}
