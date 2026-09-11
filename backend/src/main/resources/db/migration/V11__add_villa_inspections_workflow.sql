-- V11: Villa Inspections & Drone Survey Workflow
-- Tracks pre-checkin, post-checkout, routine inspections and drone aerial surveys (roof, façade, pool, grounds)

CREATE TABLE IF NOT EXISTS villa_inspections (
    id SERIAL PRIMARY KEY,
    room_id INT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    inspector_id INT REFERENCES users(id) ON DELETE SET NULL,
    inspection_type VARCHAR(50) NOT NULL DEFAULT 'ROUTINE_CHECK',
    drone_model VARCHAR(100),
    flight_altitude_meters DOUBLE PRECISION,
    flight_duration_minutes INT,
    battery_cycles INT,
    status VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    severity_level VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    checklist_results TEXT,
    media_urls TEXT,
    notes TEXT,
    action_required VARCHAR(500),
    scheduled_date TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_date TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_inspections_room ON villa_inspections(room_id);
CREATE INDEX IF NOT EXISTS idx_inspections_status ON villa_inspections(status);
CREATE INDEX IF NOT EXISTS idx_inspections_type ON villa_inspections(inspection_type);
CREATE INDEX IF NOT EXISTS idx_inspections_scheduled_date ON villa_inspections(scheduled_date);
