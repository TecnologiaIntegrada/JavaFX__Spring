package com.projetoj.purchasing.order.adapter.output.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataGoodsReceiptItemJpaRepository extends JpaRepository<GoodsReceiptItemJpaEntity, UUID> {

    List<GoodsReceiptItemJpaEntity> findAllByGoodsReceiptId(UUID goodsReceiptId);

    List<GoodsReceiptItemJpaEntity> findAllByGoodsReceiptIdIn(List<UUID> goodsReceiptIds);
}
