package com.jay.infrastructure.adapter.repository.v2;
import com.jay.domain.asset.model.*;import com.jay.domain.activity.repository.AdmissionRepository;import java.time.*;import javax.sql.DataSource;import org.springframework.jdbc.core.JdbcTemplate;
public final class MySqlAdmissionRepository implements AdmissionRepository {
 private final JdbcTemplate j;public MySqlAdmissionRepository(DataSource ds){j=new JdbcTemplate(ds);}
 private AdmissionSnapshot load(String type,String key,Deadline deadline){
  var rows=j.query("select snapshot from v2_admission_config where config_type=? and config_key=?",(rs,n)->JsonStore.read(rs.getString(1),AdmissionSnapshot.class),type,key);
  if(rows.isEmpty())throw new IllegalArgumentException("Unpublished configuration");var s=rows.getFirst();return new AdmissionSnapshot(s.activityId(),s.activityState(),s.version(),s.beginsAt(),s.endsAt(),Instant.now(),deadline.expiresAt(),s.blacklisted(),s.creditCost(),s.quota(),s.rewards());
 }
 public AdmissionSnapshot exchange(long sku,String version,Deadline deadline){return load("EXCHANGE",Long.toString(sku),deadline);}
 public AdmissionSnapshot signIn(LocalDate date,Deadline deadline){return load("SIGNIN","daily",deadline);}
}

