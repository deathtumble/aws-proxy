package uk.co.murraytait.awsproxy.aws.impl;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeTagsResult;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.Tag;
import com.amazonaws.services.elasticloadbalancing.model.TagDescription;

import uk.co.murraytait.awsproxy.CloudServiceAdaptor;
import uk.co.murraytait.awsproxy.model.ExposedService;

@Component
public class ElbAdaptor implements CloudServiceAdaptor, DisposableBean {

	private static final String ENVIRONMENT_TAG_KEY = "Environment";
	private static final String ECOSYSTEM_TAG_KEY = "Ecosystem";
	private static final String PORT_TAG_KEY = "Port";
	private static final String PATH_TAG_KEY = "Path";
	private static final String PROTOCOL_TAG_KEY = "Protocol";

	private final AmazonElasticLoadBalancing amazonElbClient;

	public ElbAdaptor(@Value("${aws.region}") String awsRegion) {
		super();

		DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();

		amazonElbClient = AmazonElasticLoadBalancingClient.builder().withCredentials(credentialsProvider)
				.withRegion(awsRegion).build();
	}

	public void destroy() throws Exception {
		amazonElbClient.shutdown();
	}

	@Override
	public Collection<ExposedService> services() throws UnknownHostException {
		SortedSet<ExposedService> services = new TreeSet<>();

		DescribeLoadBalancersRequest request = new DescribeLoadBalancersRequest();

		DescribeLoadBalancersResult response = amazonElbClient.describeLoadBalancers(request);

		Collection<String> loadBalancerNames = new LinkedList<>();
		for (LoadBalancerDescription loadBalancer : response.getLoadBalancerDescriptions()) {
			loadBalancerNames.add(loadBalancer.getLoadBalancerName());
		}

		DescribeTagsRequest describeTagsRequest = new DescribeTagsRequest();
		describeTagsRequest.withLoadBalancerNames(loadBalancerNames);
		DescribeTagsResult describeTagsResult = amazonElbClient.describeTags(describeTagsRequest);

		for (LoadBalancerDescription loadBalancer : response.getLoadBalancerDescriptions()) {
			TagDescription foundTagDescription = null;

			for (TagDescription tagDescription : describeTagsResult.getTagDescriptions()) {
				if (tagDescription.getLoadBalancerName().equals(loadBalancer.getLoadBalancerName())) {
					foundTagDescription = tagDescription;
				}
			}

			ExposedService extractExposedService = extractExposedService(loadBalancer, foundTagDescription);
			if (extractExposedService != null) {
				services.add(extractExposedService);
			}
		}

		return services;
	}

	public ExposedService extractExposedService(LoadBalancerDescription loadBalancer, TagDescription tagDescription) {
		ExposedService service = null;

		String port = getTagValue(tagDescription.getTags(), PORT_TAG_KEY);
		String path = getTagValue(tagDescription.getTags(), PATH_TAG_KEY);
		String protocol = getTagValue(tagDescription.getTags(), PROTOCOL_TAG_KEY);
		String environment = getTagValue(tagDescription.getTags(), ENVIRONMENT_TAG_KEY);
		String ecosystem = getTagValue(tagDescription.getTags(), ECOSYSTEM_TAG_KEY);

		if (!protocol.isEmpty()) {
			service = new ExposedService();

			if ("80".equals(port)) {
				service.setElbUrl(protocol.toLowerCase() + "://" + loadBalancer.getDNSName() + path);
			} else {
				service.setElbUrl(protocol.toLowerCase() + "://" + loadBalancer.getDNSName() + ":" + port + path);
			}

			service.setName(loadBalancer.getLoadBalancerName());
			service.setEnvironment(environment);
			service.setEcosystem(ecosystem);
		}

		return service;
	}

	private String getTagValue(List<Tag> tags, String tagKey) {
		Optional<Tag> foundTag = tags.stream().filter(tag -> {
			return tag.getKey().equals(tagKey);
		}).findFirst();

		if (foundTag.isPresent()) {
			return foundTag.get().getValue();
		} else {
			return "";
		}
	}
}
