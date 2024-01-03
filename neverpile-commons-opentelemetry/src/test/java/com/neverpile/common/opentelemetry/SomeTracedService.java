package com.neverpile.common.opentelemetry;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.neverpile.common.opentelemetry.Attribute.AttributeExtractor;

@Service
public class SomeTracedService {
  public static class Conv implements Function<String, String> {
    @Override
    public String apply(final String t) {
      return t + " foo";
    }
  }
  
  public static class MapExtractor implements AttributeExtractor<Map<String, Object>> {
    @Override
    public void extract(final Map<String, Object> value, final BiConsumer<String, Object> attributeCreator) {
      value.forEach(attributeCreator);
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
  public void someParams(@Attribute(name="foo") final String p1, @Attribute(name="bar") final int p2, @Attribute(name="baz") final boolean p3) {
    invocationCounter++;
  }
  
  @TraceInvocation
  public void selfNamingParam(@Attribute final String foo) {
    invocationCounter++;
  }

  @TraceInvocation
  public void someMoreParams(@Attribute(name="foo") final String p1, final int p2, final boolean p3) {
    invocationCounter++;
  }

  @TraceInvocation
  public void parameterWithConverter(@Attribute(name="foo", valueAdapter = Conv.class) final String aParam) {
    invocationCounter++;
  }
  
  @TraceInvocation
  public void parameterWithExtractor(@Attribute(attributeExtractor = MapExtractor.class) final Map<String, Object> aParam) {
    invocationCounter++;
  }

  public int getInvocationCounter() {
    return invocationCounter;
  }

  public void reset() {
    invocationCounter = 0;
  }
}
