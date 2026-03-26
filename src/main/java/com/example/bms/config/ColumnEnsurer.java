package com.example.bms.config;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class ColumnEnsurer {
    private final JdbcTemplate jdbc;
    public ColumnEnsurer(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @PostConstruct
    public void ensure() {
        Integer cnt = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'alarm_history' AND column_name = 'process_status'", Integer.class);
        if (cnt != null && cnt == 0) {
            jdbc.execute("ALTER TABLE alarm_history ADD COLUMN process_status INT NOT NULL DEFAULT 0");
        }
    }
}