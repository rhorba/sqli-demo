package com.sqli.retailflow.inventory.domain.port.in;

import com.sqli.retailflow.inventory.application.dto.StockResponse;

import java.util.List;
import java.util.UUID;

public interface GetStockUseCase {
    List<StockResponse> getAllStock();
    StockResponse getStockByProductId(UUID productId);
}
