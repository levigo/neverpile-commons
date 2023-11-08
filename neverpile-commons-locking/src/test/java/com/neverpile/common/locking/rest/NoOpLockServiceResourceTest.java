package com.neverpile.common.locking.rest;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.neverpile.common.locking.LockService.LockRequestResult;
import com.neverpile.common.locking.LockService.LockState;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT, properties = "server.error.include-message=always")
@EnableAutoConfiguration
public class NoOpLockServiceResourceTest {
  @TestConfiguration
  public static class TestConfig {
    @Bean
    public NoOpLockServiceResource lsr() {
      return new NoOpLockServiceResource();
    }
  }

  @LocalServerPort
  int port;
  
  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = port;
  }

  @Test
  public void testThat_existingLockCanBeQueried() {
    // @formatter:off
    // query existing lock
    LockState res = RestAssured.given()
      .accept(ContentType.JSON)
    .when()
      .log().all()
      .get("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract().as(LockState.class);
    
    // expect fake lock
    assertThat(res.getOwnerId()).isEqualTo("fake");
    assertThat(res.getValidUntil()).isAfter(Instant.now().plusSeconds(3600));
    // @formatter:on
  }

  @Test
  public void testThat_lockCanBeAcquired() {
    // @formatter:off
    LockRequestResult res = RestAssured.given()
      .accept(ContentType.JSON)
      .param("ownerId", "anOwnerId")
      .param("ownerName", "anOwnerName")
    .when()
      .log().all()
      .post("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(200)
      .contentType(ContentType.JSON)
      .extract().as(LockRequestResult.class);

    assertThat(res.getToken()).isEqualTo("fake");
    assertThat(res.isSuccess()).isTrue();
    assertThat(res.getState().getOwnerId()).isEqualTo("anOwnerId");
    assertThat(res.getState().getValidUntil()).isAfter(Instant.now().plusSeconds(3600));
    // @formatter:on
  }
  
  @Test
  public void testThat_lockCanBeAcquiredTwice() {
    // @formatter:off
    // fist acquire
    RestAssured.given()
        .accept(ContentType.JSON)
        .param("ownerId", "anOwnerId")
        .param("ownerName", "anOwnerName")
        .when()
        .log().all()
        .post("/api/v1/locks/aScope")
        .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON);

    // second
    RestAssured.given()
        .accept(ContentType.JSON)
        .param("ownerId", "anOwnerId")
        .param("ownerName", "anOwnerName")
        .when()
        .log().all()
        .post("/api/v1/locks/aScope")
        .then()
        .log().all()
        .statusCode(200)
        .contentType(ContentType.JSON);
    // @formatter:on
  }

  @Test
  public void testThat_lockCanBeExtended() {
    // @formatter:off
    LockState res = RestAssured.given()
      .accept(ContentType.JSON)
      .param("token", "aToken")
      .param("ownerId", "anOwnerId")
    .when()
      .log().all()
      .put("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(200)
      .extract().as(LockState.class);
    
    assertThat(res.getOwnerId()).isEqualTo("anOwnerId");
    assertThat(res.getValidUntil()).isAfter(Instant.now().plusSeconds(3600));
    // @formatter:on
  }

  public void testThat_lockCanBeReleased() {
    // @formatter:off
    RestAssured.given()
      .accept(ContentType.JSON)
      .param("token", "aToken")
    .when()
      .log().all()
      .delete("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(204);
    // @formatter:on
  }
}
