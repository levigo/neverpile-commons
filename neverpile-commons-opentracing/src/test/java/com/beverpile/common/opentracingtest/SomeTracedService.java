package com.beverpile.common.opentracingtest;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.neverpile.common.opentracing.Tag;
import com.neverpile.common.opentracing.Tag.TagExtractor;
import com.neverpile.common.opentracing.TraceInvocation;

@Service
public class SomeTracedService {
  public static class Conv implements Function<String, String> {
    @Override
    public String apply(final String t) {
      return t + " foo";
    }
  }
  
  public static class MapExtractor implements TagExtractor<Map<String, Object>> {
    @Override
    public void extract(final Map<String, Object> value, final BiConsumer<String, Object> tagCreator) {
      value.forEach(tagCreator);
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
  public void selfNamingParam(@Tag final String foo) {
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
  
  @TraceInvocation
  public void parameterWithExtractor(@Tag(tagExtractor = MapExtractor.class) final Map<String, Object> aParam) {
    invocationCounter++;
  }

  public int getInvocationCounter() {
    return invocationCounter;
  }

  public void reset() {
    invocationCounter = 0;
  }
}
