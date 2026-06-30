CREATE TABLE audit_events (
  id UUID PRIMARY KEY,
  organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
  actor_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
  event_type VARCHAR(100) NOT NULL,
  outcome VARCHAR(50) NOT NULL,
  target_type VARCHAR(100),
  target_id UUID,
  summary VARCHAR(500) NOT NULL,
  details_json TEXT NOT NULL,
  ip_address VARCHAR(64),
  user_agent VARCHAR(500),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_audit_events_org_created ON audit_events(organization_id, created_at DESC);
CREATE INDEX idx_audit_events_actor_created ON audit_events(actor_user_id, created_at DESC);
CREATE INDEX idx_audit_events_type_created ON audit_events(event_type, created_at DESC);
