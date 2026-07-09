package com.rehearsal.domain.rehearsal.port;

import com.rehearsal.domain.core.annotation.Description;
import com.rehearsal.domain.rehearsal.model.OpponentLineJob;
import java.util.Optional;

@Description("next opponent line 비동기 job 상태를 저장/조회하는 port")
public interface OpponentLineJobStore {

  void save(OpponentLineJob job);

  Optional<OpponentLineJob> findById(String sessionId, int turnNo);
}
