package uk.co.murraytait.awsproxy.model;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class ExposedService implements Comparable<ExposedService> {

	private String elbUrl;

	private String name;

	private String environment;

	private String ecosystem;

	public String getElbUrl() {
		return elbUrl;
	}

	public void setElbUrl(String elbUrl) {
		this.elbUrl = elbUrl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getEcosystem() {
		return ecosystem;
	}

	public void setEcosystem(String ecosystem) {
		this.ecosystem = ecosystem;
	}

	@Override
	public int compareTo(ExposedService other) {
		return new CompareToBuilder().append(this.ecosystem, other.ecosystem)
				.append(this.environment, other.environment).append(this.name, other.name)
				.append(this.elbUrl, other.elbUrl).toComparison();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(ecosystem).append(environment).append(name).append(elbUrl)
				.toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		ExposedService rhs = (ExposedService) obj;
		return new EqualsBuilder().appendSuper(super.equals(obj)).append(ecosystem, rhs.ecosystem)
				.append(environment, rhs.environment).append(name, rhs.name).append(elbUrl, rhs.elbUrl).isEquals();

	}
}
