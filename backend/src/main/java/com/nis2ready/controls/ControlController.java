package com.nis2ready.controls;

import com.nis2ready.common.ApiException;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/controls")
public class ControlController {
  private final ControlRepository controls;
  public ControlController(ControlRepository controls) { this.controls = controls; }
  @GetMapping
  List<Control> all() { return controls.findAllByOrderByCategoryAscCodeAsc(); }
  @GetMapping("/{id}")
  Control one(@PathVariable UUID id) { return controls.findById(id).orElseThrow(() -> ApiException.notFound("Control not found")); }
  @GetMapping("/categories")
  List<String> categories() { return controls.categories(); }
}
