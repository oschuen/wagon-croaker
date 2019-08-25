package com.github.oschuen.wagon.provider.croaker;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.wagon.TransferFailedException;
import org.codehaus.plexus.util.StringUtils;

public class RepoUrl {

    private static final String PROTOCOL_HTTP = "http://";
    private static final String PROTOCOL_HTTPS = "https://";
    private static final String PROTOCOL_SSH = "ssh://";
    private static final String PROTOCOL_REPO = "repo://";
    private static final String WEBAPP = "artifactory";

    private static final String HTTP_REGEX = PROTOCOL_HTTP + "(.*)" + "/" + WEBAPP + "/" + "(.*)";
    private static final String HTTPS_REGEX = PROTOCOL_HTTPS + "(.*)" + "/" + WEBAPP + "/" + "(.*)";
    private static final String SSH_REGEX = PROTOCOL_SSH + "(.*)" + "/" + WEBAPP + "/" + "(.*)";
    private static final String REPO_REGEX = PROTOCOL_REPO + "(.*)";

    private final String uri;
    private final String repo;

    public RepoUrl(final String url) throws TransferFailedException {
	Pattern httpPatter = Pattern.compile(HTTP_REGEX);
	Matcher httpMatcher = httpPatter.matcher(normalizeUrl(url));
	Pattern httpsPatter = Pattern.compile(HTTPS_REGEX);
	Matcher httpsMatcher = httpsPatter.matcher(normalizeUrl(url));
	Pattern sshPatter = Pattern.compile(SSH_REGEX);
	Matcher sshMatcher = sshPatter.matcher(normalizeUrl(url));
	Pattern repoPatter = Pattern.compile(REPO_REGEX);
	Matcher repoMatcher = repoPatter.matcher(normalizeUrl(url));
	if (httpsMatcher.matches()) {
	    uri = PROTOCOL_HTTPS + httpsMatcher.group(1) + "/" + WEBAPP;
	    repo = httpsMatcher.group(2);
	} else if (httpMatcher.matches()) {
	    uri = PROTOCOL_HTTP + httpMatcher.group(1) + "/" + WEBAPP;
	    repo = httpMatcher.group(2);
	} else if (sshMatcher.matches()) {
	    uri = PROTOCOL_SSH + sshMatcher.group(1) + "/" + WEBAPP;
	    repo = sshMatcher.group(2);
	} else if (repoMatcher.matches()) {
	    uri = null;
	    repo = repoMatcher.group(1);
	} else {
	    throw new TransferFailedException("Can't build RepoUrl from: " + url);
	}
    }

    private String normalizeUrl(final String url) {
	String temp = StringUtils.replace(url, "\\", "/");
	while (temp.endsWith("/")) {
	    temp = temp.substring(0, temp.length() - 1);
	}
	return temp;
    }

    /**
     * @return the uri
     */
    public String getUrl() {
	return uri;
    }

    /**
     * @return the repo
     */
    public String getRepo() {
	return repo;
    }
}
