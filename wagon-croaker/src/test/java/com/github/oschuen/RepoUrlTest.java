package com.github.oschuen;

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

import static org.junit.Assert.*;

import org.apache.maven.wagon.TransferFailedException;
import org.junit.Test;

import com.github.oschuen.wagon.provider.croaker.RepoUrl;

public class RepoUrlTest {

    @Test
    public void testRepoUrlHTTP() throws TransferFailedException {
	RepoUrl url = new RepoUrl("http://localhost:8081/artifactory/my-repo/");
	assertEquals("http://localhost:8081/artifactory", url.getUrl());
	assertEquals("my-repo", url.getRepo());
    }

    @Test
    public void testRepoUrlRepo() throws TransferFailedException {
	RepoUrl url = new RepoUrl("repo://my-repo");
	assertNull(url.getUrl());
	assertEquals("my-repo", url.getRepo());
	
    }

}
