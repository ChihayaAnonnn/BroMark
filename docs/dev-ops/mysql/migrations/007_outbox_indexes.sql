CREATE INDEX pending_type ON outbox(state,event_type,next_attempt_at,event_id);
CREATE INDEX aggregate_event ON outbox(aggregate_id,event_type,state,created_at,event_id);
CREATE INDEX pending_type ON retry_job(state,event_type,next_attempt_at,event_id);
CREATE INDEX aggregate_event ON retry_job(aggregate_id,event_type,state,created_at,event_id);
CREATE INDEX published_recovery ON retry_job(state,published_at,event_id);
