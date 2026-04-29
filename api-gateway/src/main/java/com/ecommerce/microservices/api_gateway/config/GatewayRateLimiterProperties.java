package com.ecommerce.microservices.api_gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.rate-limiter")
public class GatewayRateLimiterProperties {

	private boolean enabled = false;
	private int replenishRate = 20;
	private int burstCapacity = 40;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getReplenishRate() {
		return replenishRate;
	}

	public void setReplenishRate(int replenishRate) {
		this.replenishRate = replenishRate;
	}

	public int getBurstCapacity() {
		return burstCapacity;
	}

	public void setBurstCapacity(int burstCapacity) {
		this.burstCapacity = burstCapacity;
	}

}
