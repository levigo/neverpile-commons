package com.beverpile.common.opentracingtest;

import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.TraceInvocation;

@Service
public class SomeTracedService {
  public static class Conv implements Function<String, String> {
    @Override
    public String apply(final String t) {
      return t + " foo";
    }
  }
  
  public int invocationCounter;
  
  @TraceInvocation
  public void noParams() {
    invocationCounter++;
  }

  @TraceInvocation(operationName = "foo")
  public void weirdMethodNameReplacedByOperationName() {
    invocationCounter++;
  }
  
  @TraceInvocation
  public void someParams(@Tag(name="foo") final String p1, @Tag(name="bar") final int p2, @Tag(name="baz") final boolean p3) {
    invocationCounter++;
  }

  @TraceInvocation
  public void someMoreParams(@Tag(name="foo") final String p1, final int p2, final boolean p3) {
    invocationCounter++;
  }

  @TraceInvocation
  public void parameterWithConverter(@Tag(name="foo", valueAdapter = Conv.class) final String aParam) {
    invocationCounter++;
  }

  public int getInvocationCounter() {
    return invocationCounter;
  }

  public void reset() {
    invocationCounter = 0;
  }
}
