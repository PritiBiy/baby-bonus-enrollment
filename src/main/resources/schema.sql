CREATE TABLE enrollment (
    id            UUID          PRIMARY KEY,
    child_nric    VARCHAR(10)   NOT NULL,
    parent_nric   VARCHAR(10)   NOT NULL,
    relationship  VARCHAR(20)   NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    reason        VARCHAR(500),
    enrolled_at   TIMESTAMP WITH TIME ZONE,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE disbursement (
    id             UUID           PRIMARY KEY,
    enrollment_id  UUID           NOT NULL,
    type           VARCHAR(20)    NOT NULL,
    amount         DECIMAL(12,2)  NOT NULL,
    status         VARCHAR(20)    NOT NULL,
    processed_at   TIMESTAMP WITH TIME ZONE
);
