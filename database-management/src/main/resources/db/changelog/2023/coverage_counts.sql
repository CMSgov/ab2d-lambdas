CREATE TABLE if not exists coverage_counts
(
    id              bigserial      NOT NULL,
    contract_number varchar        NOT NULL,
    service         varchar NOT NULL,
    count           int            NOT NULL,
    year            smallint       NOT NULL,-- could cause issues in 30,744 years
    month           smallint       NOT NULL,
    create_at       timestamp      NOT NULL, -- track when the row was written
    counted_at      timestamp      NOT NULL -- sent from the services, used as a version for a series of counts, also used to find newest counts
);