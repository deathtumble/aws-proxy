package uk.co.murraytait.awsproxy.aws.impl;

import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;

import uk.co.murraytait.awsproxy.CloudServerAdaptor;
import uk.co.murraytait.awsproxy.model.Server;

@Component
public class Ec2Adaptor implements CloudServerAdaptor, DisposableBean {

	private static final String GOSS_TAG_KEY = "Goss";
	private static final String NAME_TAG_KEY = "Name";
	private static final String ENVIRONMENT_TAG_KEY = "Environment";
	private static final String ECOSYSTEM_TAG_KEY = "Ecosystem";

	private AmazonEC2 ec2;

	public Ec2Adaptor(@Value("${aws.region}") String awsRegion) {
		super();

		ec2 = AmazonEC2ClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain())
				.withRegion(awsRegion).build();
	}

	public void destroy() throws Exception {
		ec2.shutdown();
	}

	@Override
	public SortedSet<Server> servers() {
		SortedSet<Server> servers = new TreeSet<>();
		boolean done = false;

		DescribeInstancesRequest request = new DescribeInstancesRequest();

		while (!done) {
			DescribeInstancesResult response = ec2.describeInstances(request);

			for (Reservation reservation : response.getReservations()) {
				for (Instance instance : reservation.getInstances()) {
					Server server = extractServer(instance);

					servers.add(server);
				}

				request.setNextToken(response.getNextToken());

				done = response.getNextToken() == null;
			}
		}

		return servers;
	}

	private Server extractServer(Instance instance) {
		String name = getTagValue(instance, NAME_TAG_KEY);
		String environment = getTagValue(instance, ENVIRONMENT_TAG_KEY);
		String ecosystem = getTagValue(instance, ECOSYSTEM_TAG_KEY);
		boolean hasEndpoint = instance.getTags().stream()
				.anyMatch(tag -> tag.getKey().equals(GOSS_TAG_KEY) && Boolean.parseBoolean(tag.getValue()));

		Server server = new Server();

		server.setPrivateIpAddress(instance.getPrivateIpAddress());
		server.setPublicIpAddress(instance.getPublicIpAddress());
		server.setName(name);
		server.setHasEndpoint(hasEndpoint);
		server.setEnvironment(environment);
		server.setEcosystem(ecosystem);
		return server;
	}

	private String getTagValue(Instance instance, String tagKey) {
		return instance.getTags().stream().filter(tag -> {
			return tag.getKey().equals(tagKey);
		}).findFirst().get().getValue();
	}
}
