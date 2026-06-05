CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(255) NOT NULL,
  last_name VARCHAR(255) NOT NULL,
  enabled BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE organizations (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL,
  country VARCHAR(255),
  sector VARCHAR(255),
  employee_count_range VARCHAR(255),
  annual_turnover_range VARCHAR(255),
  organization_type VARCHAR(50) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE memberships (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  organization_id UUID NOT NULL REFERENCES organizations(id),
  role VARCHAR(50) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(user_id, organization_id)
);
CREATE TABLE client_organizations (
  id UUID PRIMARY KEY,
  consultancy_organization_id UUID NOT NULL REFERENCES organizations(id),
  client_organization_id UUID NOT NULL REFERENCES organizations(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  UNIQUE(consultancy_organization_id, client_organization_id)
);
CREATE TABLE questionnaires (
  id UUID PRIMARY KEY,
  type VARCHAR(100) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT,
  version INTEGER NOT NULL,
  active BOOLEAN NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE questions (
  id UUID PRIMARY KEY,
  questionnaire_id UUID NOT NULL REFERENCES questionnaires(id) ON DELETE CASCADE,
  category VARCHAR(255),
  text TEXT NOT NULL,
  description TEXT,
  weight INTEGER NOT NULL,
  recommended_evidence TEXT,
  recommended_action TEXT,
  sort_order INTEGER NOT NULL
);
CREATE TABLE controls (
  id UUID PRIMARY KEY,
  code VARCHAR(50) NOT NULL UNIQUE,
  category VARCHAR(255) NOT NULL,
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  nis2_reference TEXT,
  recommended_evidence TEXT,
  recommended_action TEXT,
  weight INTEGER NOT NULL
);
CREATE TABLE assessments (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(id),
  questionnaire_id UUID REFERENCES questionnaires(id),
  status VARCHAR(50) NOT NULL,
  overall_score INTEGER,
  risk_level VARCHAR(50),
  started_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE,
  created_by UUID REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE assessment_answers (
  id UUID PRIMARY KEY,
  assessment_id UUID NOT NULL REFERENCES assessments(id) ON DELETE CASCADE,
  control_id UUID NOT NULL REFERENCES controls(id),
  question_id UUID REFERENCES questions(id),
  category VARCHAR(255) NOT NULL,
  question_text VARCHAR(255) NOT NULL,
  weight INTEGER NOT NULL,
  recommended_evidence TEXT,
  recommended_action TEXT,
  answer VARCHAR(50) NOT NULL,
  comment TEXT,
  score DOUBLE PRECISION,
  UNIQUE(assessment_id, control_id)
);
CREATE TABLE remediation_tasks (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(id),
  title VARCHAR(255) NOT NULL,
  description TEXT NOT NULL,
  category VARCHAR(255) NOT NULL,
  priority VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  owner_user_id UUID REFERENCES users(id),
  due_date DATE,
  related_control_id UUID REFERENCES controls(id),
  related_assessment_id UUID REFERENCES assessments(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE stored_files (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(id),
  original_filename VARCHAR(500) NOT NULL,
  stored_filename VARCHAR(255) NOT NULL,
  storage_path VARCHAR(1000) NOT NULL,
  content_type VARCHAR(255) NOT NULL,
  size_bytes BIGINT NOT NULL,
  checksum_sha256 VARCHAR(64) NOT NULL,
  uploaded_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE evidence (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(id),
  title VARCHAR(255) NOT NULL,
  description TEXT,
  category VARCHAR(255),
  linked_control_id UUID REFERENCES controls(id),
  linked_task_id UUID REFERENCES remediation_tasks(id),
  file_id UUID NOT NULL REFERENCES stored_files(id),
  status VARCHAR(50) NOT NULL,
  valid_until DATE,
  uploaded_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE policy_templates (
  id UUID PRIMARY KEY,
  name VARCHAR(255) NOT NULL UNIQUE,
  category VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  disclaimer TEXT NOT NULL
);
CREATE TABLE organization_policies (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(id),
  template_id UUID REFERENCES policy_templates(id),
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE incidents (
  id UUID PRIMARY KEY,
  organization_id UUID NOT NULL REFERENCES organizations(id),
  title VARCHAR(255) NOT NULL,
  description TEXT,
  severity VARCHAR(50) NOT NULL,
  status VARCHAR(50) NOT NULL,
  detected_at TIMESTAMP WITH TIME ZONE,
  reported_internally_at TIMESTAMP WITH TIME ZONE,
  affected_systems TEXT,
  owner_user_id UUID REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE incident_actions (
  id UUID PRIMARY KEY,
  incident_id UUID NOT NULL REFERENCES incidents(id) ON DELETE CASCADE,
  description TEXT NOT NULL,
  status VARCHAR(50) NOT NULL,
  created_by UUID NOT NULL REFERENCES users(id),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  completed_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_assessments_org ON assessments(organization_id);
CREATE INDEX idx_tasks_org ON remediation_tasks(organization_id);
CREATE INDEX idx_evidence_org ON evidence(organization_id);
CREATE INDEX idx_policies_org ON organization_policies(organization_id);
CREATE INDEX idx_incidents_org ON incidents(organization_id);
