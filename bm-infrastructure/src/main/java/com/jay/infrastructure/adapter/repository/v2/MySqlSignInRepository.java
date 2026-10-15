package com.jay.infrastructure.adapter.repository.v2;
import com.jay.domain.asset.model.*;import com.jay.domain.rebate.model.*;import com.jay.domain.rebate.repository.SignInRepository;import java.time.LocalDate;import java.util.*;import javax.sql.DataSource;import org.springframework.jdbc.core.JdbcTemplate;
public final class MySqlSignInRepository implements SignInRepository {
 private final JdbcTemplate j;public MySqlSignInRepository(DataSource ds){j=new JdbcTemplate(ds);}
 public Optional<SignInResult> request(RequestIdentity id){return RequestReceipts.find(j,id,SignInResult.class);}
 public Optional<SignInResult> day(String user,LocalDate date){return j.query("select receipt from "+MySqlAssetRepository.table("sign_in_order")+" where user_id=? and business_date=?",(rs,n)->JsonStore.read(rs.getString(1),SignInResult.class),user,date).stream().findFirst();}
 public void requestReceipt(RequestIdentity id,SignInResult result){RequestReceipts.save(j,id,result.orderId(),result.state(),result);}
 public void save(String user,AdmissionSnapshot snapshot,BusinessTime time,SignInResult result){
  j.update("insert into "+MySqlAssetRepository.table("sign_in_order")+"(order_id,user_id,business_date,admission_snapshot,business_at,state,receipt) values(?,?,?,?,?,?,?)",result.orderId(),user,result.businessDate(),JsonStore.write(snapshot),java.sql.Timestamp.from(time.instant()),result.state(),JsonStore.write(result));
  for(var reward:result.rewards())j.update("insert into "+MySqlAssetRepository.table("sign_in_reward")+"(sign_in_order_id,reward_item_id,snapshot,receipt) values(?,?,?,?)",result.orderId(),reward.itemId(),JsonStore.write(snapshot.rewards().stream().filter(x->x.itemId().equals(reward.itemId())).findFirst().orElseThrow()),JsonStore.write(reward));
 }
}

