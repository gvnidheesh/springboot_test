DO
$$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'readuser') THEN
        CREATE ROLE readuser LOGIN PASSWORD '${migrationReaderPassword}';
    END IF;
END
$$;

GRANT USAGE ON SCHEMA migration TO readuser;
GRANT SELECT ON ALL TABLES IN SCHEMA migration TO readuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA migration GRANT SELECT ON TABLES TO readuser;
