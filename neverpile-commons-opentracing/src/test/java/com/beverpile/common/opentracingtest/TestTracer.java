package com.beverpile.common.opentracingtest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tag;

public class TestTracer implements Tracer {
  public final class ASpanBuilder implements SpanBuilder {
    private final Map<String, Object> tags = new HashMap<>();
    private final String operationName;

    public ASpanBuilder(final String operationName) {
      this.operationName = operationName;
    }

    @Override
    public <T> SpanBuilder withTag(final Tag<T> tag, final T value) {
      tags.put(tag.getKey(), value);
      return this;
    }

    @Override
    public SpanBuilder withTag(final String key, final Number value) {
      tags.put(key, value);
      return this;
    }

    @Override
    public SpanBuilder withTag(final String key, final boolean value) {
      tags.put(key, value);
      return this;
    }

    @Override
    public SpanBuilder withTag(final String key, final String value) {
      tags.put(key, value);
      return this;
    }

    @Override
    public SpanBuilder withStartTimestamp(final long microseconds) {
      return this; // not used
    }

    @Override
    public Span start() {
      return new ASpan(tags, operationName);
    }

    @Override
    public SpanBuilder ignoreActiveSpan() {
      return this; // not used
    }

    @Override
    public SpanBuilder asChildOf(final Span parent) {
      return this; // not used
    }

    @Override
    public SpanBuilder asChildOf(final SpanContext parent) {
      return this; // not used
    }

    @Override
    public SpanBuilder addReference(final String referenceType, final SpanContext referencedContext) {
      return this; // not used
    }
  }

  public final class AScopeManager implements ScopeManager {
    private Span span;
    private Scope scope;

    @Override
    public Span activeSpan() {
      return span;
    }

    @Override
    public Scope activate(final Span span) {
      scope = new AScope(span);
      return null;
    }
  }

  public final class AScope implements Scope {
    private final Span span;

    public AScope(final Span span) {
      this.span = span;
    }

    @Override
    public void close() {
      // nothing to do
    }
  }

  public final class ASpan implements Span {
    private final Map<String, Object> tags;
    private String operationName;
  
    public ASpan(final Map<String, Object> tags, final String operationName) {
      this.tags = tags;
      this.operationName = operationName;
    }
  
    @Override
    public <T> Span setTag(final Tag<T> tag, final T value) {
      tags.put(tag.getKey(), value);
      return this;
    }
  
    @Override
    public Span setTag(final String key, final Number value) {
      tags.put(key, value);
      return this;
    }
  
    @Override
    public Span setTag(final String key, final boolean value) {
      tags.put(key, value);
      return this;
    }
  
    @Override
    public Span setTag(final String key, final String value) {
      tags.put(key, value);
      return this;
    }
  
    @Override
    public Span setOperationName(final String operationName) {
      this.operationName = operationName;
      return this;
    }
  
    @Override
    public Span setBaggageItem(final String key, final String value) {
      return this; // not used
    }
  
    @Override
    public Span log(final long timestampMicroseconds, final String event) {
      return this; // not used
    }
  
    @Override
    public Span log(final long timestampMicroseconds, final Map<String, ?> fields) {
      return this; // not used
    }
  
    @Override
    public Span log(final String event) {
      return this; // not used
    }
  
    @Override
    public Span log(final Map<String, ?> fields) {
      return this; // not used
    }
  
    @Override
    public String getBaggageItem(final String key) {
      return null; // not used
    }
  
    @Override
    public void finish(final long finishMicros) {
      finishedSpans.add(this);
    }
  
    @Override
    public void finish() {
      finishedSpans.add(this);
    }
  
    @Override
    public SpanContext context() {
      return null; // not used
    }

    public Map<String, Object> getTags() {
      return tags;
    }

    public String getOperationName() {
      return operationName;
    }
  }

  private final AScopeManager scopeManager = new AScopeManager();
  
  private final List<ASpan> finishedSpans = new ArrayList<>();
  
  @Override
  public ScopeManager scopeManager() {
    return scopeManager;
  }

  @Override
  public Span activeSpan() {
    return scopeManager().activeSpan();
  }

  @Override
  public Scope activateSpan(final Span span) {
    return scopeManager().activate(span);
  }

  @Override
  public SpanBuilder buildSpan(final String operationName) {
    return new ASpanBuilder(operationName);
  }

  @Override
  public <C> void inject(final SpanContext spanContext, final Format<C> format, final C carrier) {
    // not used
  }

  @Override
  public <C> SpanContext extract(final Format<C> format, final C carrier) {
    // not used
    return null;
  }

  @Override
  public void close() {
    // not used
  }

  public List<ASpan> getFinishedSpans() {
    return finishedSpans;
  }
}
