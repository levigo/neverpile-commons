package com.neverpile.common.opentelemetry;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class OpenTelemetryAspectTest {
  @Autowired
  TestTracer tt;

  @Autowired
  SomeTracedService service;

  @BeforeEach
  public void reset() {
    service.reset();
    tt.getFinishedSpans().clear();
  }

  @Test
  void testNoParams() {
    service.noParams();

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getOperationName()).isEqualTo("SomeTracedService.noParams");
    assertThat(tt.getFinishedSpans().get(0).getAttributes()).isEmpty();
  }

  @Test
  void testRenamedOp() {
    service.weirdMethodNameReplacedByOperationName();

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getOperationName()).isEqualTo("foo");
    assertThat(tt.getFinishedSpans().get(0).getAttributes()).isEmpty();
  }

  @Test
  void testParams() {
    service.someParams("hello", 4711, true);

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getAttributes()) //
        .containsEntry("foo", "hello") //
        .containsEntry("bar", 4711L) //
        .containsEntry("baz", true);
  }
  
  @Test
  void testSelfNamingParam() {
    service.selfNamingParam("hello");
    
    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getAttributes()) //
    .containsEntry("foo", "hello"); //
  }

  @Test
  void testPartialParams() {
    service.someMoreParams("hello", 4711, true);

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getAttributes()).containsEntry("foo", "hello").hasSize(1);
  }
  
  @Test
  void testAttributeExtractor() {
    HashMap<String, Object> m = new HashMap<>();
    m.put("foo", "bar");
    m.put("bar", 1234L);
    m.put("baz", 1234.56d);
    m.put("buz", false);
    
    service.parameterWithExtractor(m);

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getAttributes()) //
        .containsEntry("foo", "bar") //
        .containsEntry("bar", 1234L) //
        .containsEntry("baz", 1234.56d) //
        .containsEntry("buz", false);
  }
}
