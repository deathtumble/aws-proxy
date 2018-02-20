package uk.co.murraytait.awsproxy.model;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Server implements Comparable<Server> {

	private String publicIpAddress;

	private String privateIpAddress;

	private String name;

	private String environment;

	private String ecosystem;

	public String getEcosystem() {
		return ecosystem;
	}

	public void setEcosystem(String ecosystem) {
		this.ecosystem = ecosystem;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	private boolean hasEndpoint;

	public String getPublicIpAddress() {
		return publicIpAddress;
	}

	public void setPublicIpAddress(String publicIpAddress) {
		this.publicIpAddress = publicIpAddress;
	}

	public String getPrivateIpAddress() {
		return privateIpAddress;
	}

	public void setPrivateIpAddress(String privateIpAddress) {
		this.privateIpAddress = privateIpAddress;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isHasEndpoint() {
		return hasEndpoint;
	}

	public void setHasEndpoint(boolean hasEndpoint) {
		this.hasEndpoint = hasEndpoint;
	}

	@Override
	public int compareTo(Server other) {
		return new CompareToBuilder().append(this.ecosystem, other.ecosystem)
				.append(this.environment, other.environment).append(this.name, other.name)
				.append(this.privateIpAddress, other.privateIpAddress).toComparison();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(ecosystem).append(environment).append(hasEndpoint).append(name)
				.append(privateIpAddress).append(publicIpAddress).toHashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != getClass()) {
			return false;
		}
		Server rhs = (Server) obj;
		return new EqualsBuilder().appendSuper(super.equals(obj)).append(ecosystem, rhs.ecosystem)
				.append(environment, rhs.environment).append(name, rhs.name)
				.append(privateIpAddress, rhs.privateIpAddress).append(publicIpAddress, rhs.publicIpAddress)
				.append(hasEndpoint, rhs.hasEndpoint).isEquals();

	}

}
