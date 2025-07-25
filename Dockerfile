# Use the official Keycloak image as base
FROM quay.io/keycloak/keycloak:24.0.0 as builder

# Set environment variables for the build
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Copy our custom authenticator JAR to the providers directory
COPY target/keycloak-nin-autolink-1.0.0.jar /opt/keycloak/providers/

# Build the custom Keycloak distribution
RUN /opt/keycloak/bin/kc.sh build

# Create the runtime image
FROM quay.io/keycloak/keycloak:24.0.0

# Copy the built custom distribution
COPY --from=builder /opt/keycloak/ /opt/keycloak/

# Set environment variables for runtime
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true

# Expose the default Keycloak port
EXPOSE 8080

# Set the entrypoint
ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev"]