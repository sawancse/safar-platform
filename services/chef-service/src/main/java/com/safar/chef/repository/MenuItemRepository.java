package com.safar.chef.repository;

import com.safar.chef.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByMenuId(UUID menuId);

    List<MenuItem> findByMenuIdOrderBySortOrder(UUID menuId);
}
