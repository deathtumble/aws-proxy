package uk.co.murraytait.awsproxy.aws.impl;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.DescribeClustersRequest;
import com.amazonaws.services.ecs.model.DescribeClustersResult;
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClient;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetHealthRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealth;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthDescription;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetHealthStateEnum;

import uk.co.murraytait.awsproxy.CloudServiceAdaptor;
import uk.co.murraytait.awsproxy.model.ServiceSummary;

@Component
public class ElbAdaptor implements CloudServiceAdaptor, DisposableBean {

	private static final String ENVIRONMENT_TAG_KEY = "Environment";
	private static final String PRODUCT_TAG_KEY = "Product";
	private static final String NAME_TAG_KEY = "Name";

	private final AmazonElasticLoadBalancing amazonElbClient;

	private final AmazonECS amazonEcsClient;

	private final Function<? super Service, ? extends ServiceSummary> serviceMapper;

	public ElbAdaptor(@Value("${aws.region}") String awsRegion) {
		super();

		DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();

		amazonElbClient = AmazonElasticLoadBalancingClient.builder().withCredentials(credentialsProvider)
				.withRegion(awsRegion).build();

		amazonEcsClient = AmazonECSClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain())
				.withRegion(awsRegion).build();

		serviceMapper = service -> new ServiceSummary().withName(service.getServiceName())
				.withDesiredTasks(service.getDesiredCount()).withRunningTasks(service.getRunningCount());
	}

	public void destroy() throws Exception {
		amazonElbClient.shutdown();
	}

	@Override
	public Collection<ServiceSummary> serviceSummaries(String[] clusterNames) throws UnknownHostException {
		Map<String, ServiceSummary> serviceSummaryMap = createExposedServices(clusterNames);

		List<TargetGroup> targetGroups = getTargetGroups();

		for (TargetGroup targetGroup : targetGroups) {
			List<Tag> tags = amazonElbClient.describeTags(new DescribeTagsRequest().withResourceArns(targetGroup.getTargetGroupArn())).getTagDescriptions().get(0).getTags();
			
			String serviceName = getTagValue(tags, NAME_TAG_KEY);
			ServiceSummary serviceSummary = serviceSummaryMap.get(serviceName);

			serviceSummary.setEnvironment(getTagValue(tags, ENVIRONMENT_TAG_KEY));
			serviceSummary.setProduct(getTagValue(tags, PRODUCT_TAG_KEY));
			
			addTargetGroupsHealth(targetGroup, serviceSummary);
		}

		return serviceSummaryMap.values();
	}

	private void addTargetGroupsHealth(TargetGroup targetGroup, ServiceSummary serviceSummary) {
		List<TargetHealthDescription> targetHealthDescriptions = amazonElbClient.describeTargetHealth(new DescribeTargetHealthRequest()
				.withTargetGroupArn(targetGroup.getTargetGroupArn())).getTargetHealthDescriptions();

		for (TargetHealthDescription targetHealthDescription : targetHealthDescriptions) {

			TargetHealth targetHealth = targetHealthDescription.getTargetHealth();

			serviceSummary.incrementTargets();
			
			if (TargetHealthStateEnum.Healthy.equals(TargetHealthStateEnum.fromValue(targetHealth.getState()))) {
				serviceSummary.incrementHealthyTargets();
			}
		}
	}

	private List<TargetGroup> getTargetGroups() {
		DescribeTargetGroupsRequest request = new DescribeTargetGroupsRequest();
		DescribeTargetGroupsResult describeTargetGroups = amazonElbClient.describeTargetGroups(request);
		List<TargetGroup> targetGroups = describeTargetGroups.getTargetGroups();
		return targetGroups;
	}

	private Map<String, ServiceSummary> createExposedServices(String... clusterNames) {
		List<Service> ecsServices = new LinkedList<>();

		DescribeClustersRequest describeClustersRequest = new DescribeClustersRequest().withClusters(clusterNames);
		DescribeClustersResult describeClusters = amazonEcsClient.describeClusters(describeClustersRequest);
		describeClusters.getClusters();
		for (Cluster cluster : describeClusters.getClusters()) {
			String clusterName = cluster.getClusterName();

			DescribeServicesRequest request = new DescribeServicesRequest();
			request.withCluster(clusterName);
			request.withServices(clusterNames);
			ecsServices.addAll(amazonEcsClient.describeServices(request).getServices());
		}

		return ecsServices.stream().map(serviceMapper).collect(Collectors.toMap(ServiceSummary::getName, p -> p));
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
