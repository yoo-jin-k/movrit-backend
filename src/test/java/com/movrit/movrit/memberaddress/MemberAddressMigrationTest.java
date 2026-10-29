package com.movrit.movrit.memberaddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** 통합 DB의 격리 스키마에서 기존 주소 데이터를 보존하면서 컬럼 확장을 검증합니다. */
@EnabledIfEnvironmentVariable(named = "MOVRIT_MEMBER_ADDRESS_MIGRATION_TEST", matches = "true")
class MemberAddressMigrationTest {

  @Test
  void widensLegacyAddressColumnsWithoutChangingExistingValues() throws Exception {
    String url =
        System.getenv()
            .getOrDefault(
                "INTEGRATION_DB_URL", "jdbc:postgresql://localhost:5432/movrit_integration");
    assertTrue(url.endsWith("/movrit_integration"), "통합 DB에서만 실행합니다.");
    try (Connection connection =
        DriverManager.getConnection(
            url,
            System.getenv().getOrDefault("INTEGRATION_DB_USERNAME", "movrit_user"),
            System.getenv("INTEGRATION_DB_PASSWORD"))) {
      connection.setAutoCommit(false);
      try {
        String schema = "qa_member_address_" + UUID.randomUUID().toString().replace("-", "");
        execute(connection, "CREATE SCHEMA " + schema);
        execute(connection, "SET LOCAL search_path TO " + schema);
        execute(connection, "CREATE TABLE member(id BIGINT PRIMARY KEY)");
        execute(
            connection,
            "CREATE TABLE member_address("
                + "id BIGSERIAL PRIMARY KEY, member_id BIGINT NOT NULL REFERENCES member(id), "
                + "address_name VARCHAR(100), receiver_name VARCHAR(100) NOT NULL, "
                + "receiver_phone VARCHAR(30) NOT NULL, postal_code VARCHAR(20) NOT NULL, "
                + "address1 VARCHAR(255) NOT NULL, address2 VARCHAR(255), "
                + "is_default BOOLEAN NOT NULL DEFAULT FALSE, created_at TIMESTAMP NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL)");
        execute(connection, "INSERT INTO member VALUES(1)");
        String legacyAddress = "가".repeat(255);
        insertAddress(connection, legacyAddress, legacyAddress);

        String migration = Files.readString(Path.of("docs/sql/feature-member-address.sql"));
        execute(connection, migration);
        execute(connection, migration);
        String maximumAddress = "나".repeat(300);
        insertAddress(connection, maximumAddress, maximumAddress);

        assertEquals(
            255, scalar(connection, "SELECT length(address1) FROM member_address WHERE id=1"));
        assertEquals(
            300, scalar(connection, "SELECT length(address2) FROM member_address WHERE id=2"));
      } finally {
        connection.rollback();
      }
    }
  }

  private static void insertAddress(Connection connection, String address1, String address2)
      throws Exception {
    try (var statement =
        connection.prepareStatement(
            "INSERT INTO member_address(member_id,address_name,receiver_name,receiver_phone,"
                + "postal_code,address1,address2,created_at,updated_at) "
                + "VALUES(1,'집','수령인','010-0000-0000','06000',?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)")) {
      statement.setString(1, address1);
      statement.setString(2, address2);
      statement.executeUpdate();
    }
  }

  private static void execute(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement()) {
      statement.execute(sql);
    }
  }

  private static long scalar(Connection connection, String sql) throws Exception {
    try (var statement = connection.createStatement();
        var rows = statement.executeQuery(sql)) {
      assertTrue(rows.next());
      return rows.getLong(1);
    }
  }
}
