package com.jay.domain.asset.repository;
import com.jay.domain.asset.model.*;
import java.util.function.Supplier;
public interface UserTransactionRunner {
 <T>T inUser(String user,WorkClass work,Deadline deadline,Supplier<T> action);
 <T>T inControl(WorkClass work,Deadline deadline,Supplier<T> action);
 <T>T inSlot(RouteSlot slot,WorkClass work,Deadline deadline,Supplier<T> action);
}
