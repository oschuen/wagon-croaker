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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.maven.wagon.AbstractWagon;
import org.apache.maven.wagon.CommandExecutionException;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.Streams;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.events.TransferEvent;
import org.apache.maven.wagon.providers.ssh.ScpHelper;
import org.apache.maven.wagon.resource.Resource;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.codehaus.plexus.util.cli.CommandLineUtils;
import org.codehaus.plexus.util.cli.Commandline;

/**
 * Croaker deployer using "external" jfrog program.
 *
 * @author oliver
 * @plexus.component role="org.apache.maven.wagon.Wagon" role-hint="croaker"
 *                   instantiation-strategy="per-lookup"
 */
public class CroakerWagon extends AbstractWagon {

	private final String roleHint = "croaker";

	/**
	 * The external jFrog command to use - default is <code>jfrog</code>.
	 *
	 * @component.configuration default="jfrog"
	 */
	private final String jfrogExecutable = "jfrog";

	@Override
	public void get(final String resourceName, final File destination)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		final String path = StringUtils.replace(resourceName, "\\", "/");

		final Resource resource = new Resource(path);

		fireGetInitiated(resource, destination);

		createParentDirectories(destination);

		fireGetStarted(resource, destination);

		try {
			final String[] args = { getUrl().getRepo() + "/" + resourceName,
					destination.getCanonicalPath() };
			executeCommand("dl", args, false);
		} catch (final Exception e) {
			fireTransferError(resource, e, TransferEvent.REQUEST_GET);
			throw new TransferFailedException("Error executing command for transfer", e);
		}

		postProcessListeners(resource, destination, TransferEvent.REQUEST_GET);

		fireGetCompleted(resource, destination);
	}

	@Override
	public boolean getIfNewer(final String resourceName, final File destination,
			final long timestamp)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		fireSessionDebug(
				"getIfNewer in Frog wagon is not supported - performing an unconditional get");
		get(resourceName, destination);
		return true;
	}

	@Override
	public void put(final File source, final String destination)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		final Resource resource = new Resource(destination);

		firePutInitiated(resource, source);

		if (!source.exists()) {
			throw new ResourceDoesNotExistException(
					"Specified source file does not exist: " + source);
		}

		final String resourceName = StringUtils.replace(destination, "\\", "/");

		resource.setContentLength(source.length());

		resource.setLastModified(source.lastModified());

		firePutStarted(resource, source);

		postProcessListeners(resource, source, TransferEvent.REQUEST_PUT);

		try {
			fireSessionDebug("Push: " + source.getCanonicalPath() + " To " + destination);
			final String[] args = { source.getCanonicalPath(),
					getUrl().getRepo() + "/" + resourceName };
			executeCommand("u", args, false);
		} catch (final Exception e) {
			fireTransferError(resource, e, TransferEvent.REQUEST_PUT);

			throw new TransferFailedException("Error executing command for transfer", e);
		}
		firePutCompleted(resource, source);
	}

	private Streams executeCommand(final String attent, final String[] args,
			final boolean ignoreFailures)
			throws CommandExecutionException, TransferFailedException {

		File privateKey;
		try {
			privateKey = ScpHelper.getPrivateKey(authenticationInfo);
		} catch (final FileNotFoundException e) {
			throw new CommandExecutionException(e.getMessage(), e);
		}
		final Commandline cl = createFrogCommand(jfrogExecutable, attent, privateKey);

		for (final String arg : args) {
			cl.createArg().setValue(arg);
		}

		fireSessionDebug("Executing command: " + cl.toString());

		try {
			final CommandLineUtils.StringStreamConsumer out = new CommandLineUtils.StringStreamConsumer();
			final CommandLineUtils.StringStreamConsumer err = new CommandLineUtils.StringStreamConsumer();
			final int exitCode = CommandLineUtils.executeCommandLine(cl, out, err);
			final Streams streams = new Streams();
			streams.setOut(out.getOutput());
			streams.setErr(err.getOutput());
			fireSessionDebug(streams.getOut());
			fireSessionDebug(streams.getErr());
			if (!(exitCode == 0 || ignoreFailures)) {
				throw new CommandExecutionException("Command [" + cl.toString() + "] Exit code "
						+ exitCode + " - " + err.getOutput());
			}
			return streams;
		} catch (final CommandLineException e) {
			throw new CommandExecutionException("Error executing command line", e);
		}
	}

	private RepoUrl getUrl() throws TransferFailedException {
		final String url = getRepository().getUrl();
		if (url == null) {
			throw new TransferFailedException("Url not set");
		}
		return new RepoUrl(url.substring((roleHint + ":").length()));
	}

	private Commandline createFrogCommand(final String executable, final String attent,
			final File privateKey) throws CommandExecutionException, TransferFailedException {
		try {
			final Commandline cl = new Commandline();

			cl.addEnvironment("JFROG_CLI_OFFER_CONFIG", "false");

			cl.setExecutable(executable);

			cl.createArg().setValue("rt");

			cl.createArg().setValue(attent);

			final RepoUrl url = getUrl();

			if (url.getUrl() != null) {
				cl.createArg().setValue("--url=" + url.getUrl());

				if (privateKey != null) {
					cl.createArg().setValue("--ssh-key-path=" + privateKey.getCanonicalPath());
				}

				final String password = authenticationInfo.getPassword();
				if (password != null) {
					cl.createArg().setValue("--password=" + password + "");
				}

				final String user = authenticationInfo.getUserName();
				if (user != null) {
					cl.createArg().setValue("--user=" + user);
				}
			}
			return cl;
		} catch (final IOException e) {
			throw new CommandExecutionException("private Key not found", e);
		}
	}

	@Override
	protected void openConnectionInternal() throws ConnectionException, AuthenticationException {
		if (authenticationInfo == null) {
			authenticationInfo = new AuthenticationInfo();
		}
	}

	@Override
	protected void closeConnection() throws ConnectionException {
		// NOOP
	}
}
