package com.synexis.management_service.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {

            statement.execute("CREATE EXTENSION IF NOT EXISTS postgis");

            statement.execute("""
                        DO $$
                        BEGIN
                            IF EXISTS (
                                SELECT 1 FROM information_schema.tables
                                WHERE table_name = 'partners'
                            ) THEN
                                CREATE INDEX IF NOT EXISTS idx_partners_location
                                ON partners USING GIST (location);
                            END IF;
                        END
                        $$;
                    """);

        }
    }
}