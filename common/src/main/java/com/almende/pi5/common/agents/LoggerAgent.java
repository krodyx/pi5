/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common.agents;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.pi5.common.LogLine;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class LoggerAgent.
 */
@Access(AccessType.PUBLIC)
public class LoggerAgent extends Agent {
	private static final Logger	LOG		= Logger.getLogger(LoggerAgent.class
												.getName());
	List<LogLine>				logs	= Collections
												.synchronizedList(new ArrayList<LogLine>());
	URI							graphs	= URI.create("wsclient:ws://localhost:3000/agents/graphs");

	@Override
	public void onReady() {
		final ObjectNode config = getConfig();

		this.graphs = config.hasNonNull("clientUrl") ? URI.create(config.get(
				"clientUrl").asText()) : null;
	}

	/**
	 * Log.
	 *
	 * @param ll
	 *            the ll
	 */
	public void log(@Name("logline") final LogLine ll) {
		logs.add(ll);
		forward(ll);
	}

	/**
	 * Forward.
	 *
	 * @param ll
	 *            the ll
	 */
	public void forward(@Name("logline") final LogLine ll) {
		final Params params = new Params();
		final ArrayList<LogLine> array = new ArrayList<LogLine>(1);
		array.add(ll);
		params.add("data", array);
		final JSONRequest request = new JSONRequest("addData", params);
		try {
			caller.call(graphs, request);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Graphs agent not connected...");
		}
	}

	/**
	 * Forward all.
	 */
	public void forwardAll() {
		final Params params = new Params();
		params.add("data", logs);
		final JSONRequest request = new JSONRequest("addData", params);
		try {
			caller.call(graphs, request);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Graphs agent not connected...");
		}
	}

	/**
	 * Gets the json.
	 *
	 * @return the json
	 */
	@JsonIgnore
	public ArrayNode getJson() {
		final ArrayNode result = JOM.createArrayNode();
		synchronized (logs) {
			for (LogLine ll : logs) {
				result.add(JOM.getInstance().valueToTree(ll));
			}
		}
		return result;
	}

	/**
	 * Gets the csv file.
	 *
	 * @return the csv file
	 */
	@JsonIgnore
	public String getCsvFile() {
		final StringBuilder sb = new StringBuilder();
		synchronized (logs) {
			for (LogLine ll : logs) {
				sb.append(ll.getCsv());
			}
		}
		return sb.toString();
	}

	/**
	 * Clear.
	 */
	public void clear() {
		logs = Collections.synchronizedList(new ArrayList<LogLine>());
	}
}
