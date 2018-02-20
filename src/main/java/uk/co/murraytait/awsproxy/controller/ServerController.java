package uk.co.murraytait.awsproxy.controller;

import java.util.SortedSet;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.co.murraytait.awsproxy.CloudServerAdaptor;
import uk.co.murraytait.awsproxy.model.Server;

@RestController
public class ServerController {

	private final CloudServerAdaptor serverAdaptor;

	public ServerController(CloudServerAdaptor serverAdaptor) {
		super();
		this.serverAdaptor = serverAdaptor;
	}

	@RequestMapping("/servers")
	public SortedSet<Server> servers() {
		return serverAdaptor.servers();
	}
}
