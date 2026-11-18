-- ==============================================================================
-- V2__add_interview_slots_and_schedules.sql: UT-06 Đặt lịch phỏng vấn & nhắc lịch
-- ==============================================================================

CREATE TABLE IF NOT EXISTS interview_slots (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES interview_rounds(id) ON DELETE CASCADE,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'PICKED', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_interview_slots_time_order CHECK (end_at > start_at)
);

CREATE INDEX IF NOT EXISTS idx_interview_slots_round_id ON interview_slots(round_id);
CREATE INDEX IF NOT EXISTS idx_interview_slots_status ON interview_slots(status);

CREATE TABLE IF NOT EXISTS interview_schedules (
    id UUID PRIMARY KEY,
    round_id UUID NOT NULL REFERENCES interview_rounds(id) ON DELETE CASCADE,
    slot_id UUID NOT NULL REFERENCES interview_slots(id) ON DELETE CASCADE,
    meet_url VARCHAR(500) NOT NULL,
    reminded_24h_at TIMESTAMPTZ,
    reminded_1h_at TIMESTAMPTZ,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED'
        CHECK (status IN ('SCHEDULED', 'DONE', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_interview_schedules_round_id ON interview_schedules(round_id);
CREATE INDEX IF NOT EXISTS idx_interview_schedules_slot_id ON interview_schedules(slot_id);
CREATE INDEX IF NOT EXISTS idx_interview_schedules_status ON interview_schedules(status);
