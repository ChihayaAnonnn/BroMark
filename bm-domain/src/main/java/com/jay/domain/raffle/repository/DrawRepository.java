package com.jay.domain.raffle.repository;
import com.jay.domain.raffle.model.*;import com.jay.domain.asset.model.*;import com.jay.domain.task.repository.*;import java.util.*;
public interface DrawRepository extends RecoveryAggregatePort {Optional<String> request(RequestIdentity id);DrawRecord find(String order,boolean lock);void insert(RequestIdentity id,DrawRecord order);boolean update(DrawRecord before,DrawRecord after);void award(DrawRecord order,String status);void delivered(DrawRecord order);}
