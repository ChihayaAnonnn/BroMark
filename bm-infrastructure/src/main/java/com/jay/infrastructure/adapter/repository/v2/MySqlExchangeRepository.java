package com.jay.infrastructure.adapter.repository.v2;
import com.jay.domain.asset.model.*;import com.jay.domain.exchange.model.*;import com.jay.domain.exchange.repository.ExchangeRepository;import java.util.*;import javax.sql.DataSource;import org.springframework.jdbc.core.JdbcTemplate;
public final class MySqlExchangeRepository implements ExchangeRepository {
 private final JdbcTemplate j;public MySqlExchangeRepository(DataSource ds){j=new JdbcTemplate(ds);}
 public Optional<ExchangeResult> request(RequestIdentity id){return RequestReceipts.find(j,id,ExchangeResult.class);}
 public Optional<ExchangeResult> order(String user,String order){return j.query("select user_id,receipt from "+MySqlAssetRepository.table("credit_exchange_order")+" where order_id=?",(rs,n)->{if(!user.equals(rs.getString(1)))throw new AssetOwnershipException();return JsonStore.read(rs.getString(2),ExchangeResult.class);},order).stream().findFirst();}
 public void save(RequestIdentity id,AdmissionSnapshot snapshot,BusinessTime time,ExchangeResult result){
  j.update("insert into "+MySqlAssetRepository.table("credit_exchange_order")+"(order_id,user_id,request_id,request_hash,admission_snapshot,credit_cost,quota_snapshot,business_at,business_date,business_month,state,receipt) values(?,?,?,?,?,?,?,?,?,?,?,?)",result.orderId(),id.userId(),id.requestId(),id.requestHash(),JsonStore.write(snapshot),snapshot.creditCost(),JsonStore.write(snapshot.quota()),java.sql.Timestamp.from(time.instant()),time.businessDate(),time.businessMonth().toString(),result.state(),JsonStore.write(result));RequestReceipts.save(j,id,result.orderId(),result.state(),result);
 }
}

