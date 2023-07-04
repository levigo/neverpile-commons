package com.neverpile.common.locking.rest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.BAD_REQUEST,
    reason = "Can not contest this lock. (There is no lock for this scope or you are the locks owner)")
public class NoContestException extends Exception {
  private static final long serialVersionUID = 1L;
}
