package com.movrit.movrit.seller.dto;

import com.movrit.movrit.preorder.entity.PreorderStatus;
import java.math.BigDecimal;
import java.util.List;

public final class SellerDashboardDtos {
  private SellerDashboardDtos() {}

  public record Summary(
      long projects,
      long activeProjects,
      long orders,
      BigDecimal paidGmv,
      BigDecimal refundAmount,
      BigDecimal estimatedSettlement) {}

  public record Project(
      Long id,
      String title,
      PreorderStatus status,
      long orderedQuantity,
      long targetQuantity,
      double achievementRate,
      BigDecimal paidGmv,
      BigDecimal refundAmount) {}

  public record Response(Summary summary, List<Project> projects) {}
}
