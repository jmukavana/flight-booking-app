package com.resilire.apigateway.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuotaRegistryTest {

    @Test
    void unregistered_tenant_uses_default_limits() {
        QuotaRegistry registry = new QuotaRegistry(5, 5);

        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (registry.tryConsume("some-key-hash")) allowed++;
        }

        assertThat(allowed).isEqualTo(5);
    }

    @Test
    void registered_tenant_uses_explicit_limits() {
        QuotaRegistry registry = new QuotaRegistry(5, 5);
        registry.register("enterprise-key-hash", 500, 20);

        int allowed = 0;
        for (int i = 0; i < 25; i++) {
            if (registry.tryConsume("enterprise-key-hash")) allowed++;
        }

        assertThat(allowed).isEqualTo(20);
    }

    @Test
    void different_tenants_have_independent_buckets() {
        QuotaRegistry registry = new QuotaRegistry(3, 3);

        for (int i = 0; i < 3; i++) registry.tryConsume("tenant-a");
        assertThat(registry.tryConsume("tenant-a")).isFalse();
        assertThat(registry.tryConsume("tenant-b")).isTrue();
    }

    @Test
    void register_updates_limit_for_known_tenant() {
        QuotaRegistry registry = new QuotaRegistry(2, 2);
        registry.register("key-hash", 100, 10);

        assertThat(registry.registeredTenants()).isEqualTo(1);

        int allowed = 0;
        for (int i = 0; i < 15; i++) {
            if (registry.tryConsume("key-hash")) allowed++;
        }
        assertThat(allowed).isEqualTo(10);
    }
}
