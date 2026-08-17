package com.movrit.movrit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:movrit;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "jwt.secret=movrit-test-secret-key-12345678901234567890",
        "kakao.client-id=test",
        "kakao.client-secret=test",
        "kakao.redirect-uri=http://localhost/test",
        "google.client-id=test",
        "google.client-secret=test",
        "google.redirect-uri=http://localhost/test"
})
class PersistenceSmokeTest {

    @Test
    void contextLoadsWithAllJpaMappingsAndRepositories() {
        // ApplicationContext 시작 자체가 엔티티 매핑과 Spring Data Repository 파싱 검증이다.
    }
}
