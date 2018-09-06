package uk.co.murraytait.awsproxy.aws.impl;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import com.amazonaws.services.ecs.model.DescribeServicesRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import com.amazonaws.services.ecs.model.ListServicesRequest;
import com.amazonaws.services.ecs.model.ListServicesResult;
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

    private static final String NAME_TAG_KEY = "Name";

    private final AmazonElasticLoadBalancing amazonElbClient;

    private final AmazonECS amazonEcsClient;

    public ElbAdaptor(@Value("${aws.region}") String awsRegion) {
        super();

        DefaultAWSCredentialsProviderChain credentialsProvider = new DefaultAWSCredentialsProviderChain();

        amazonElbClient = AmazonElasticLoadBalancingClient.builder().withCredentials(credentialsProvider)
                .withRegion(awsRegion).build();

        amazonEcsClient = AmazonECSClientBuilder.standard().withCredentials(new DefaultAWSCredentialsProviderChain())
                .withRegion(awsRegion).build();
    }

    public void destroy() throws Exception {
        amazonElbClient.shutdown();
    }

    @Override
    public Collection<ServiceSummary> serviceSummaries() throws UnknownHostException {
        Map<String, ServiceSummary> serviceSummaryMap = createExposedServices();

        List<TargetGroup> targetGroups = getTargetGroups();

        for (TargetGroup targetGroup : targetGroups) {
            List<Tag> tags = amazonElbClient
                    .describeTags(new DescribeTagsRequest().withResourceArns(targetGroup.getTargetGroupArn()))
                    .getTagDescriptions().get(0).getTags();

            String serviceName = getTagValue(tags, NAME_TAG_KEY);
            if (serviceSummaryMap.containsKey(serviceName)) {
                ServiceSummary serviceSummary = serviceSummaryMap.get(serviceName);

                addTargetGroupsHealth(targetGroup, serviceSummary);
            }
        }

        return serviceSummaryMap.values();
    }

    private void addTargetGroupsHealth(TargetGroup targetGroup, ServiceSummary serviceSummary) {
        List<TargetHealthDescription> targetHealthDescriptions = amazonElbClient
                .describeTargetHealth(
                        new DescribeTargetHealthRequest().withTargetGroupArn(targetGroup.getTargetGroupArn()))
                .getTargetHealthDescriptions();

        for (TargetHealthDescription targetHealthDescription : targetHealthDescriptions) {
            
            serviceSummary.setTargeted();

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

    private Map<String, ServiceSummary> createExposedServices() {
        List<Service> ecsServices = new LinkedList<>();

        List<String> clusterArns = getClusterArns();
        
        Map<String, ServiceSummary> serviceSummaries = new HashMap<>();

        for (String clusterArn : clusterArns) {
            List<Service> services = getServices(clusterArn);
            ecsServices.addAll(services);

            for (Service service : services) {
                String serviceName = service.getServiceName();
                
                ServiceSummary serviceSummary = new ServiceSummary().withName(serviceName)
                        .withDesiredTasks(service.getDesiredCount()).withRunningTasks(service.getRunningCount());
                
                String environment = serviceName.substring(serviceName.lastIndexOf("-") + 1); 
                
                serviceSummary.setEnvironment(environment);
                
                serviceSummaries.put(serviceSummary.getName(), serviceSummary);
            }
        }
        
        return serviceSummaries;
    }

    private List<Service> getServices(String clusterArn) {
        List<String> serviceArns = getServicesArns(clusterArn);
        List<Service> services = Collections.emptyList();

        if (!serviceArns.isEmpty()) {
            services = getServices(clusterArn, serviceArns);

        }
        return services;
    }

    private List<Service> getServices(String clusterArn, List<String> serviceArns) {
        List<Service> services;
        DescribeServicesRequest request = new DescribeServicesRequest();
        request.withCluster(clusterArn);
        request.withServices(serviceArns);
        services = amazonEcsClient.describeServices(request).getServices();
        return services;
    }

    private List<String> getServicesArns(String clusterArn) {
        ListServicesRequest listServicesRequest = new ListServicesRequest();
        listServicesRequest.setCluster(clusterArn);

        ListServicesResult listServices = amazonEcsClient.listServices(listServicesRequest);
        List<String> serviceArns = listServices.getServiceArns();
        return serviceArns;
    }

    private List<String> getClusterArns() {
        ListClustersResult listClusters = amazonEcsClient.listClusters();
        List<String> clusterArns = listClusters.getClusterArns();
        return clusterArns;
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
