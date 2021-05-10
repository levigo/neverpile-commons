/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.neverpile.common.actuate;

import java.util.Properties;

import org.springframework.boot.actuate.info.GitInfoContributor;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.info.GitProperties;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * An extended {@link InfoContributor} that exposes git-describe info and tags.
 */
public class ExtendedGitInfoContributor extends GitInfoContributor {

  public ExtendedGitInfoContributor(GitProperties properties, Mode mode) {
    super(properties, mode);
  }

  @Override
  protected PropertySource<?> toSimplePropertySource() {
    Properties props = new Properties();
    copyIfSet(props, "branch");
    copyIfSet(props, "tags");

    String commitId = getProperties().getShortCommitId();
    if (commitId != null) {
      props.put("commit.id", commitId);
    }
    String describe = getProperties().get("commit.id.describe");
    if (describe != null) {
      props.put("describe", describe);
    }

    copyIfSet(props, "commit.time");
    return new PropertiesPropertySource("git", props);
  }
}
