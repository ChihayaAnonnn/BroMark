package com.jay.domain.rebate.model;
import java.time.LocalDate;import com.jay.domain.asset.model.Deadline;
public record SignInCommand(String userId,String requestId,LocalDate targetDate,Deadline deadline) {}

