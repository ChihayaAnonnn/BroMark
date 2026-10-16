package com.jay.domain.task.model;

import java.util.List;

public record RecoveryScanPage(List<String> ids, RecoveryScanPosition next, boolean exhausted) {
  public RecoveryScanPage {
    ids = List.copyOf(ids);
  }
}
