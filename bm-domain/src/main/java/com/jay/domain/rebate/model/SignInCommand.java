package com.jay.domain.rebate.model;

import com.jay.domain.asset.model.Deadline;
import java.time.LocalDate;

public record SignInCommand(
    String userId, String requestId, LocalDate targetDate, Deadline deadline) {}
