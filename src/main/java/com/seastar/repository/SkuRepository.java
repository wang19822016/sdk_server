package com.seastar.repository;

import com.seastar.model.Sku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Created by osx on 17/3/14.
 */
public interface SkuRepository extends JpaRepository<Sku, Integer> {
    Sku findByAppIdAndSku(Integer appId, String sku);
    List<Sku> findAllByAppId(Integer appId);
}
