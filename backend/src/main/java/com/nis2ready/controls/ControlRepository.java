package com.nis2ready.controls;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;

public interface ControlRepository extends JpaRepository<Control, UUID> {
  List<Control> findAllByOrderByCategoryAscCodeAsc();
  @Query("select distinct c.category from Control c order by c.category")
  List<String> categories();
}
