package com.beverpile.common.opentracingtest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class OpentracingAspectTest {
  @Autowired
  TestTracer tt;

  @Autowired
  SomeTracedService service;

  @Before
  public void reset() {
    service.reset();
    tt.getFinishedSpans().clear();
  }

  @Test
  public void testNoParams() throws Exception {
    service.noParams();

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getOperationName()).isEqualTo("SomeTracedService.noParams");
    assertThat(tt.getFinishedSpans().get(0).getTags()).isEmpty();
  }

  @Test
  public void testRenamedOp() throws Exception {
    service.weirdMethodNameReplacedByOperationName();

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getOperationName()).isEqualTo("foo");
    assertThat(tt.getFinishedSpans().get(0).getTags()).isEmpty();
  }

  @Test
  public void testParams() throws Exception {
    service.someParams("hello", 4711, true);

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getTags()) //
        .containsEntry("foo", "hello") //
        .containsEntry("bar", 4711) //
        .containsEntry("baz", true);
  }

  @Test
  public void testPartialParams() throws Exception {
    service.someMoreParams("hello", 4711, true);

    assertThat(service.getInvocationCounter()).isEqualTo(1);
    assertThat(tt.getFinishedSpans()).hasSize(1);
    assertThat(tt.getFinishedSpans().get(0).getTags()).containsEntry("foo", "hello").hasSize(1);
  }
}
