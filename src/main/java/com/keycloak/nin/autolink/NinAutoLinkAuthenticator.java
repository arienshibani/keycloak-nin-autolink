package com.keycloak.nin.autolink;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Keycloak Authenticator that automatically links federated IdP accounts
 * to local users based on NiN (Norwegian Identity Number) matching.
 *
 * This authenticator is designed to replace the "Verify Existing Account by Re-authentication"
 * step in the First Broker Login flow.
 */
public class NinAutoLinkAuthenticator implements Authenticator {

    private static final Logger logger = LoggerFactory.getLogger(NinAutoLinkAuthenticator.class);

    // Configuration constants
    private static final String NIN_CLAIM_NAME = "nin";
    private static final String NIN_ATTRIBUTE_NAME = "nin";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.debug("Starting NiN auto-link authentication for user session: {}",
                    context.getAuthenticationSession().getParentSession().getId());

        try {
            // Check if we're in a broker login context
            if (!isBrokerLoginContext(context)) {
                logger.debug("Not in broker login context, attempting normal flow");
                context.attempted();
                return;
            }

            // Extract NiN from brokered identity
            String nin = extractNinFromBrokeredIdentity(context);
            if (nin == null || nin.trim().isEmpty()) {
                logger.debug("No NiN found in brokered identity claims");
                context.attempted();
                return;
            }

            logger.debug("Found NiN in brokered identity: {}", maskNin(nin));

            // Find local user by NiN
            UserModel localUser = findUserByNin(context, nin);
            if (localUser == null) {
                logger.debug("No local user found with NiN: {}", maskNin(nin));
                context.attempted();
                return;
            }

            logger.debug("Found local user: {}", localUser.getUsername());

            // Check if user has no stored credentials
            if (hasStoredCredentials(context, localUser)) {
                logger.debug("User {} has stored credentials, cannot auto-link", localUser.getUsername());
                context.attempted();
                return;
            }

            // Auto-link the user
            logger.info("Auto-linking user {} with NiN: {}", localUser.getUsername(), maskNin(nin));
            context.setUser(localUser);
            context.success();

        } catch (Exception e) {
            logger.error("Error during NiN auto-link authentication", e);
            context.attempted();
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // No action required for this authenticator
        context.attempted();
    }

    @Override
    public boolean requiresUser() {
        return false; // We're trying to find the user
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true; // Always configured
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // No required actions
    }

    @Override
    public void close() {
        // No resources to close
    }

    /**
     * Check if we're in a broker login context
     */
    private boolean isBrokerLoginContext(AuthenticationFlowContext context) {
        // Check if we're in a broker login by looking for broker session notes
        return context.getAuthenticationSession().getClientNotes().containsKey("BROKER_SESSION_ID") ||
               context.getAuthenticationSession().getUserSessionNotes().containsKey("BROKER_SESSION_ID");
    }

        /**
     * Extract NiN from the brokered identity context
     */
    private String extractNinFromBrokeredIdentity(AuthenticationFlowContext context) {
        try {
            // Try to get NiN from various possible sources
            String nin = null;

            // Check in user attributes from brokered identity
            if (context.getUser() != null) {
                nin = context.getUser().getFirstAttribute(NIN_ATTRIBUTE_NAME);
            }

            // Check in brokered context notes
            if (nin == null) {
                nin = context.getAuthenticationSession().getUserSessionNotes().get(NIN_CLAIM_NAME);
            }

            // Check in client notes
            if (nin == null) {
                nin = context.getAuthenticationSession().getClientNotes().get(NIN_CLAIM_NAME);
            }

            return nin;

        } catch (Exception e) {
            logger.warn("Error extracting NiN from brokered identity", e);
            return null;
        }
    }

    /**
     * Find a local user by NiN (Norwegian Identity Number)
     */
    private UserModel findUserByNin(AuthenticationFlowContext context, String nin) {
        try {
            UserProvider userProvider = context.getSession().users();
            RealmModel realm = context.getRealm();

            // First, try to find by username (NiN might be the username)
            UserModel user = userProvider.getUserByUsername(realm, nin);
            if (user != null) {
                logger.debug("Found user by username matching NiN: {}", user.getUsername());
                return user;
            }

                        // For now, we'll only support username matching
            // In a production environment, you might want to implement a more efficient
            // attribute-based search using Keycloak's query capabilities
            logger.debug("User not found by username, attribute search not implemented");

            return null;

        } catch (Exception e) {
            logger.error("Error finding user by NiN: {}", maskNin(nin), e);
            return null;
        }
    }

    /**
     * Check if a user has stored credentials (password)
     */
    private boolean hasStoredCredentials(AuthenticationFlowContext context, UserModel user) {
        try {
            // Check if user has a password set
            return user.credentialManager().isConfiguredFor("password");
        } catch (Exception e) {
            logger.warn("Error checking stored credentials for user: {}", user.getUsername(), e);
            // If we can't determine, assume they have credentials for safety
            return true;
        }
    }

    /**
     * Mask NiN for logging (show only first 6 and last 2 digits)
     */
    private String maskNin(String nin) {
        if (nin == null || nin.length() < 8) {
            return "***";
        }
        return nin.substring(0, 6) + "***" + nin.substring(nin.length() - 2);
    }
}