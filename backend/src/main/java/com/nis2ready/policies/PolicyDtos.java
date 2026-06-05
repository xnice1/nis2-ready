package com.nis2ready.policies;

public class PolicyDtos {
  public record PolicyUpdateRequest(String title, String content, PolicyStatus status) {}
}
