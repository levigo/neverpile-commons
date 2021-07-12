package com.neverpile.common.locking.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.NOT_FOUND, reason = "No such lock")
public class NotFoundException extends Exception {
  private static final long serialVersionUID = 1L;
}
