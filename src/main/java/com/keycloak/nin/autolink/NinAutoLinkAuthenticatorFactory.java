package com.keycloak.nin.autolink;

import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * Factory class for the NiN Auto-Link Authenticator.
 *
 * This factory registers the authenticator with Keycloak's SPI system
 * and provides metadata about the authenticator.
 */
public class NinAutoLinkAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "nin-auto-link";
    public static final String DISPLAY_TYPE = "NiN Auto-Link";
    public static final String REFERENCE_CATEGORY = "broker";

    private static final AuthenticationExecutionModel.Requirement[] REQUIREMENT_CHOICES = {
        AuthenticationExecutionModel.Requirement.REQUIRED,
        AuthenticationExecutionModel.Requirement.DISABLED
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return DISPLAY_TYPE;
    }

    @Override
    public String getReferenceCategory() {
        return REFERENCE_CATEGORY;
    }

    @Override
    public boolean isConfigurable() {
        return false; // No configuration needed for this authenticator
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Automatically links federated IdP accounts to local users based on NiN (Norwegian Identity Number) matching. " +
               "This authenticator should be used in the First Broker Login flow to replace the password verification step " +
               "when a local user with matching NiN has no stored credentials.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        // No configuration properties needed
        return List.of();
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new NinAutoLinkAuthenticator();
    }

    @Override
    public void init(org.keycloak.Config.Scope config) {
        // No initialization needed
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-initialization needed
    }

    @Override
    public void close() {
        // No resources to close
    }
}
