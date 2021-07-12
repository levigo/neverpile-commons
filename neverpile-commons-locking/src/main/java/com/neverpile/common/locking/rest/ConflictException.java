package com.neverpile.common.locking.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT, reason = "Locked by other party")
public class ConflictException extends Exception {
  private static final long serialVersionUID = 1L;
}
