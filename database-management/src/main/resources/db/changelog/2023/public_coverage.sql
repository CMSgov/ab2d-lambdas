CREATE SCHEMA IF NOT EXISTS public;

CREATE TABLE IF NOT EXISTS public.coverage(
                                              bene_coverage_period_id       INTEGER      NOT NULL,
                                              bene_coverage_search_event_id BIGINT       NOT NULL,
                                              contract                      VARCHAR(32)  NOT NULL,
                                              year                          INTEGER      NOT NULL,
                                              month                         INTEGER      NOT NULL,
                                              beneficiary_id                BIGINT       NOT NULL,
                                              current_mbi                   VARCHAR(32),
                                              historic_mbis                 VARCHAR(256),
                                              opt_out_flag                  BOOLEAN      DEFAULT 'FALSE',
                                              effective_date                TIMESTAMP
);
