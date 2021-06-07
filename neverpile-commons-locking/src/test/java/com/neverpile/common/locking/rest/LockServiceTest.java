package com.neverpile.common.locking.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.BDDMockito;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.neverpile.common.locking.LockService;
import com.neverpile.common.locking.LockService.LockRequestResult;
import com.neverpile.common.locking.LockService.LockState;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
    webEnvironment = WebEnvironment.RANDOM_PORT, properties = "server.error.include-message=always")
@EnableAutoConfiguration
public class LockServiceTest {
  @TestConfiguration
  public static class TestConfig {
    @Bean
    public LockServiceResource lsr() {
      return new LockServiceResource();
    }
  }

  @LocalServerPort
  int port;
  
  @MockBean
  LockService mockLockService;

  @BeforeEach
  public void setupRestAssured() {
    RestAssured.port = port;
  }

  @Test
  public void testThat_existingLockCanBeQueried() throws Exception {
    // @formatter:off
    Instant anInstant = Instant.now();
    
    BDDMockito
      .given(mockLockService.queryLock(any()))
      .willReturn(Optional.of(new LockState("anOwnerId", "anOwnerName", anInstant)));
    
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
    
    assertThat(res.getOwnerId()).isEqualTo("anOwnerId");
    assertThat(res.getOwnerName()).isEqualTo("anOwnerName");
    assertThat(res.getValidUntil()).isEqualTo(anInstant);
    // @formatter:on
  }
  
  @Test
  public void testThat_nonexistingLockCanBeQueried() throws Exception {
    // @formatter:off
    BDDMockito
      .given(mockLockService.queryLock(any()))
      .willReturn(Optional.empty());
    
    // query existing lock
    RestAssured.given()
        .accept(ContentType.JSON)
      .when()
        .log().all()
        .get("/api/v1/locks/aScope")
      .then()
        .log().all()
        .statusCode(404);
    // @formatter:on
  }

  @Test
  public void testThat_lockCanBeAcquired() throws Exception {
    // @formatter:off
    ArgumentCaptor<String> scopeC = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> ownerIdC = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> ownerNameC = ArgumentCaptor.forClass(String.class);

    Instant anInstant = Instant.now();
    
    BDDMockito
      .given(mockLockService.tryAcquireLock(scopeC.capture(), ownerIdC.capture(), ownerNameC.capture()))
      .willReturn(new LockRequestResult(true, "aToken", new LockState("anOwnerId", "anOwnerName", anInstant)));
    
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

    assertThat(res.getToken()).isEqualTo("aToken");
    assertThat(res.isSuccess()).isTrue();
    assertThat(res.getState().getOwnerId()).isEqualTo("anOwnerId");
    assertThat(res.getState().getOwnerName()).isEqualTo("anOwnerName");
    assertThat(res.getState().getValidUntil()).isEqualTo(anInstant);
    
    assertThat(scopeC.getValue()).isEqualTo("aScope");
    assertThat(ownerIdC.getValue()).isEqualTo("anOwnerId");
    assertThat(ownerNameC.getValue()).isEqualTo("anOwnerName");
    // @formatter:on
  }

  @Test
  public void testThat_lockConflictIsSignalled() throws Exception {
    // @formatter:off
    BDDMockito
      .given(mockLockService.tryAcquireLock(any(), any(), any()))
      .willReturn(new LockRequestResult(false, "aToken", null));
    
    // store collection
    RestAssured.given()
        .accept(ContentType.JSON)
        .param("ownerId", "anOwnerId")
        .param("ownerName", "anOwnerName")
      .when()
        .log().all()
        .post("/api/v1/locks/aScope")
      .then()
        .log().all()
        .statusCode(409);
    // @formatter:on
  }

  @Test
  public void testThat_lockCanBeExtended() throws Exception {
    // @formatter:off
    ArgumentCaptor<String> scopeC = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> tokenC = ArgumentCaptor.forClass(String.class);
    
    Instant anInstant = Instant.now();

    BDDMockito
      .given(mockLockService.extendLock(scopeC.capture(), tokenC.capture()))
      .willReturn(new LockState("anOwnerId", "anOwnerName", anInstant));
    
    LockState res = RestAssured.given()
      .accept(ContentType.JSON)
      .param("token", "aToken")
    .when()
      .log().all()
      .put("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(200)
      .extract().as(LockState.class);
    
    assertThat(res.getOwnerId()).isEqualTo("anOwnerId");
    assertThat(res.getOwnerName()).isEqualTo("anOwnerName");
    assertThat(res.getValidUntil()).isEqualTo(anInstant);
    
    assertThat(scopeC.getValue()).isEqualTo("aScope");
    assertThat(tokenC.getValue()).isEqualTo("aToken");
    // @formatter:on
  }

  @Test
  public void testThat_lockLossIsSignalled() throws Exception {
    // @formatter:off
    BDDMockito
      .given(mockLockService.extendLock(any(), any()))
      .willThrow(new LockService.LockLostException());
    
    RestAssured.given()
      .accept(ContentType.JSON)
      .param("token", "aToken")
    .when()
      .log().all()
      .put("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(410);
    // @formatter:on
  }

  public void testThat_lockCanBeReleased() throws Exception {
    // @formatter:off
    ArgumentCaptor<String> scopeC = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<String> tokenC = ArgumentCaptor.forClass(String.class);

    BDDMockito
      .doNothing().when(mockLockService).releaseLock(scopeC.capture(), tokenC.capture());
    
    RestAssured.given()
      .accept(ContentType.JSON)
      .param("token", "aToken")
    .when()
      .log().all()
      .delete("/api/v1/locks/aScope")
    .then()
      .log().all()
      .statusCode(204);
    
    assertThat(scopeC.getValue()).isEqualTo("aScope");
    assertThat(tokenC.getValue()).isEqualTo("aToken");
    // @formatter:on
  }
}
