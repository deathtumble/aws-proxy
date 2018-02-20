package uk.co.murraytait.awsproxy;

import java.util.SortedSet;

import uk.co.murraytait.awsproxy.model.Server;

public interface CloudServerAdaptor {

	SortedSet<Server> servers();
}
