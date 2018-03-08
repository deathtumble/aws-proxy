package uk.co.murraytait.awsproxy;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.SortedSet;

import uk.co.murraytait.awsproxy.model.ExposedService;

public interface CloudServiceAdaptor {

	Collection<ExposedService> services() throws UnknownHostException;

}
