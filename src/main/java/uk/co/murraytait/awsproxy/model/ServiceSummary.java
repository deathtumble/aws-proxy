package uk.co.murraytait.awsproxy.model;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ServiceSummary implements Comparable<ServiceSummary> {

	private String name;

	private String environment;

	private String product;
	
	private Integer desiredTasks;
	
	private Integer runningTasks;
	
	private Integer targets;

	private Integer healthyTargets;
	
	private boolean isTargeted;

	public ServiceSummary() {
		super();
		healthyTargets = 0;
		targets = 0;
		isTargeted = false;
	}
	
	public void incrementHealthyTargets() {
		healthyTargets++;
	}

	public void incrementTargets() {
		targets++;
	}

	public Integer getTargets() {
		return targets;
	}

	public Integer getHealthyTargets() {
		return healthyTargets;
	}

	public Integer getDesiredTasks() {
		return desiredTasks;
	}

	public void setDesiredTasks(Integer desiredTasks) {
		this.desiredTasks = desiredTasks;
	}

	public ServiceSummary withDesiredTasks(Integer desiredTasks) {
		this.desiredTasks = desiredTasks;
		return this;
	}

	public Integer getRunningTasks() {
		return runningTasks;
	}

	public void setRunningTasks(Integer runningTasks) {
		this.runningTasks = runningTasks;
	}

	public ServiceSummary withRunningTasks(Integer runningTasks) {
		this.runningTasks = runningTasks;
		return this;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ServiceSummary withName(String name) {
		this.name = name;
		return this;
	}

	public String getProduct() {
		return product;
	}

	public void setProduct(String product) {
		this.product = product;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public ServiceSummary withEnvironment(String environment) {
		this.environment = environment;
		return this;
	}

    public boolean isTargeted() {
        return isTargeted;
    }

    public void setTargeted() {
        this.isTargeted = true;
    }

    public void resetTargeted() {
        this.isTargeted = false;
    }

    @Override
	public int compareTo(ServiceSummary other) {
		return new CompareToBuilder().append(this.product, other.product)
				.append(this.environment, other.environment).append(this.name, other.name)
				.append(this.desiredTasks, other.desiredTasks).toComparison();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(product).append(environment).append(name).append(desiredTasks)
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ServiceSummary rhs = (ServiceSummary) obj;
		return new EqualsBuilder().appendSuper(super.equals(obj)).append(product, rhs.product)
				.append(environment, rhs.environment).append(name, rhs.name).append(desiredTasks, rhs.desiredTasks).isEquals();

	}

}
