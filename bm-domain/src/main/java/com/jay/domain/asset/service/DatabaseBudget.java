package com.jay.domain.asset.service;
import com.jay.domain.asset.model.*;
public interface DatabaseBudget {Permit acquire(String physicalDb,WorkClass work,Deadline deadline);int available(String physicalDb,WorkClass work);}
