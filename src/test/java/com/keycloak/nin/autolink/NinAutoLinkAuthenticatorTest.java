package com.keycloak.nin.autolink;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NinAutoLinkAuthenticatorTest {

    @Mock
    private AuthenticationFlowContext context;

    @Mock
    private KeycloakSession session;

    @Mock
    private RealmModel realm;

    @Mock
    private UserModel user;

    @Mock
    private UserProvider userProvider;

    @Mock
    private AuthenticationSessionModel authSession;

    private NinAutoLinkAuthenticator authenticator;

    @BeforeEach
    void setUp() {
        authenticator = new NinAutoLinkAuthenticator();

        // Setup common mocks
        when(context.getSession()).thenReturn(session);
        when(context.getRealm()).thenReturn(realm);
        when(context.getAuthenticationSession()).thenReturn(authSession);
        when(session.users()).thenReturn(userProvider);
    }

    @Test
    void testAuthenticate_NotBrokerLoginContext_ShouldAttempt() {
        // Given
        when(authSession.getClientNotes()).thenReturn(new HashMap<>());
        when(authSession.getUserSessionNotes()).thenReturn(new HashMap<>());

        // When
        authenticator.authenticate(context);

        // Then
        verify(context).attempted();
        verify(context, never()).setUser(any());
        verify(context, never()).success();
    }

    @Test
    void testAuthenticate_NoNinInClaims_ShouldAttempt() {
        // Given
        setupBrokerLoginContext();
        setupNoNinInClaims();

        // When
        authenticator.authenticate(context);

        // Then
        verify(context).attempted();
        verify(context, never()).setUser(any());
        verify(context, never()).success();
    }

    @Test
    void testAuthenticate_UserNotFoundByNin_ShouldAttempt() {
        // Given
        setupBrokerLoginContext();
        setupNinInClaims("12345678901");
        when(userProvider.getUserByUsername(realm, "12345678901")).thenReturn(null);

        // When
        authenticator.authenticate(context);

        // Then
        verify(context).attempted();
        verify(context, never()).setUser(any());
        verify(context, never()).success();
    }

    @Test
    void testAction_ShouldAttempt() {
        // When
        authenticator.action(context);

        // Then
        verify(context).attempted();
    }

    @Test
    void testRequiresUser_ShouldReturnFalse() {
        // When
        boolean result = authenticator.requiresUser();

        // Then
        assert !result;
    }

    @Test
    void testConfiguredFor_ShouldReturnTrue() {
        // When
        boolean result = authenticator.configuredFor(session, realm, user);

        // Then
        assert result;
    }

    @Test
    void testSetRequiredActions_ShouldDoNothing() {
        // When
        authenticator.setRequiredActions(session, realm, user);

        // Then
        // No exceptions should be thrown
    }

    @Test
    void testClose_ShouldDoNothing() {
        // When
        authenticator.close();

        // Then
        // No exceptions should be thrown
    }

    // Helper methods for test setup
    private void setupBrokerLoginContext() {
        Map<String, String> clientNotes = new HashMap<>();
        clientNotes.put("BROKER_SESSION_ID", "test-broker-session");
        when(authSession.getClientNotes()).thenReturn(clientNotes);
    }

    private void setupNoNinInClaims() {
        when(context.getUser()).thenReturn(null);
        when(authSession.getUserSessionNotes()).thenReturn(new HashMap<>());
    }

    private void setupNinInClaims(String nin) {
        Map<String, String> userSessionNotes = new HashMap<>();
        userSessionNotes.put("nin", nin);
        when(authSession.getUserSessionNotes()).thenReturn(userSessionNotes);
    }
}
