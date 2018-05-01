package uk.co.murraytait.awsproxy;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.SortedSet;

import uk.co.murraytait.awsproxy.model.ServiceSummary;

public interface CloudServiceAdaptor {

	Collection<ServiceSummary> serviceSummaries() throws UnknownHostException;

}
