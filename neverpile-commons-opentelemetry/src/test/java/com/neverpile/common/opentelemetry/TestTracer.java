package com.neverpile.common.opentelemetry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;


public class TestTracer implements Tracer {
  public final class ASpanBuilder implements SpanBuilder {

    private SpanContext spanContext; // not used
    private final Map<String, Object> attributes = new HashMap<>();
    private final String operationName;

    public ASpanBuilder(final String operationName) {
      this.operationName = operationName;
    }

    @Override
    public Span startSpan() {
      return new ASpan(attributes, operationName);
    }

    @Override
    public SpanBuilder setParent(Context context) {
      return this; // not used
    }

    @Override
    public SpanBuilder setNoParent() {
      return this; // not used
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext) {
      return this; // not used
    }

    @Override
    public SpanBuilder addLink(SpanContext spanContext, Attributes attributes) {
      return this; // not used
    }

    @Override
    public SpanBuilder setAttribute(String key, String value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, long value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, double value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public SpanBuilder setAttribute(String key, boolean value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public <T> SpanBuilder setAttribute(AttributeKey<T> key, T value) {
      return this; // not used
    }

    @Override
    public SpanBuilder setSpanKind(SpanKind spanKind) {
      return this;  // not used
    }

    @Override
    public SpanBuilder setStartTimestamp(long startTimestamp, TimeUnit unit) {
      return this;  // not used
    }

  }

  public final class ASpan implements Span {
    private final Map<String, Object> attributes;
    private final String operationName;
    private StatusCode statusCode;
    private Throwable exception;

    public ASpan(final Map<String, Object> attributes, final String operationName) {
      this.attributes = attributes;
      this.operationName = operationName;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public String getOperationName() {
      return operationName;
    }

    public StatusCode getStatusCode() {
      return statusCode;
    }

    public Throwable getException() {
      return exception;
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
      attributes.put(key.getKey(), value);
      return this;
    }

    @Override
    public Span setAttribute(String key, long value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public Span setAttribute(String key, double value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public Span setAttribute(String key, boolean value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public Span setAttribute(String key, String value) {
      attributes.put(key, value);
      return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
      return null; // not used
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
      return null; // not used
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
      this.statusCode = statusCode;
      return null;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
      this.exception = exception;
      return null;
    }

    @Override
    public Span updateName(String name) {
      return null;  // not used
    }

    @Override
    public void end() {
      finishedSpans.add(this);
    }

    @Override
    public void end(long timestamp, TimeUnit unit) {
      // not used
    }

    @Override
    public SpanContext getSpanContext() {
      return null; // not used
    }

    @Override
    public boolean isRecording() {
      return false; // not used
    }
  }

  private final List<ASpan> finishedSpans = new ArrayList<>();

  @Override
  public SpanBuilder spanBuilder(final String operationName) {
    return new ASpanBuilder(operationName);
  }

  public List<ASpan> getFinishedSpans() {
    return finishedSpans;
  }
}
