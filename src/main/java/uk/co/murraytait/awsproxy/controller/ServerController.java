package uk.co.murraytait.awsproxy.controller;

import java.net.UnknownHostException;
import java.util.Collection;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.co.murraytait.awsproxy.CloudServerAdaptor;
import uk.co.murraytait.awsproxy.CloudServiceAdaptor;
import uk.co.murraytait.awsproxy.model.ServiceSummary;
import uk.co.murraytait.awsproxy.model.Server;

@RestController
public class ServerController {

	private final CloudServerAdaptor serverAdaptor;
	
	private final CloudServiceAdaptor serviceAdaptor;

	public ServerController(CloudServerAdaptor serverAdaptor, CloudServiceAdaptor serviceAdaptor) {
		super();
		this.serverAdaptor = serverAdaptor;
		this.serviceAdaptor = serviceAdaptor;
	}

	@RequestMapping("/servers")
	public Collection<Server> servers() throws UnknownHostException {
		int tries = 0;
		Collection<Server> servers = null;

		while (servers == null) {
			tries++;

			try {
				servers = serverAdaptor.servers();
			} catch (UnknownHostException exception) {
				if (tries > 4) {
					throw exception;
				}
			}
		}

		return servers;
	}

	@RequestMapping("/services")
	public Collection<ServiceSummary> services() throws UnknownHostException {
		int tries = 0;
		Collection<ServiceSummary> services = null;

		while (services == null) {
			tries++;

			try {
				services = serviceAdaptor.serviceSummaries();
			} catch (UnknownHostException exception) {
				if (tries > 4) {
					throw exception;
				}
			}
		}

		return services;
	}
}
