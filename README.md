# Keycloak NiN Auto-Link Authenticator

A custom Keycloak Authenticator SPI that automatically links federated IdP accounts to local users based on NiN (Norwegian Identity Number) matching. This authenticator replaces the "Verify Existing Account by Re-authentication" step in the First Broker Login flow.

## 🔧 Background

When using Keycloak as an Identity Provider (IdP) broker with external providers like Signicat via OIDC, the default "First Broker Login" flow requires users to verify their local account credentials (typically via password) before linking the federated account. This creates friction for users who:

- Have no password set
- Should be auto-linked based on shared identity attributes (NiN)

## 🎯 Features

This authenticator automatically links a federated IdP account to a local Keycloak user when:

- The federated IdP provides a claim with the user's NiN (e.g., `"nin"` in OIDC claims)
- A local user with a matching `username` or `nin` attribute exists
- That local user has **no stored credentials** (i.e., no password set)

## 🏗️ Building the Plugin

### Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker (for building the custom Keycloak image)

### Build Steps

1. **Clone the repository:**

   ```bash
   git clone https://github.com/arienshibani/keycloak-nin-autolink
   cd keycloak-nin-autolink
   ```

2. **Build the JAR file:**

   ```bash
   mvn clean package
   ```

3. **Build the custom Keycloak Docker image:**

   ```bash
   docker build -t keycloak-nin-autolink:latest .
   ```

## 🚀 Deployment

### Option 1: Docker Deployment

1. **Run the custom Keycloak container:**

   ```bash
   docker run -p 8080:8080 \
     -e KEYCLOAK_ADMIN=admin \
     -e KEYCLOAK_ADMIN_PASSWORD=admin \
     keycloak-nin-autolink:latest
   ```

2. **Access Keycloak Admin Console:**

   - URL: <http://localhost:8080>
   - Username: `admin`
   - Password: `admin`

### Option 2: Manual Deployment

1. **Copy the JAR to your Keycloak installation:**

   ```bash
   cp target/keycloak-nin-autolink-1.0.0.jar /path/to/keycloak/providers/
   ```

2. **Build Keycloak with the plugin:**

   ```bash
   cd /path/to/keycloak
   bin/kc.sh build
   ```

3. **Start Keycloak:**

   ```bash
   bin/kc.sh start-dev
   ```

## ⚙️ Configuration

### 1. Create a Custom Authentication Flow

1. **Log into Keycloak Admin Console**
2. **Navigate to:** Authentication → Flows
3. **Create a new flow:**
   - Click "Create"
   - Name: `NiN Auto-Link First Broker Login`
   - Description: `Custom First Broker Login with NiN auto-linking`
   - Built-in: `No`

### 2. Configure the Flow Steps

1. **Add the NiN Auto-Link authenticator:**
   - Click "Add execution"
   - Select "NiN Auto-Link" from the dropdown
   - Set requirement to "REQUIRED"
   - Click "Save"

2. **Add fallback authentication:**
   - Click "Add execution"
   - Select "Verify Existing Account by Re-authentication"
   - Set requirement to "ALTERNATIVE"
   - Click "Save"

3. **Set up the flow structure:**

   ```bash
   NiN Auto-Link First Broker Login
   ├── NiN Auto-Link (REQUIRED)
   └── Verify Existing Account by Re-authentication (ALTERNATIVE)
   ```

### 3. Configure Identity Provider

1. **Navigate to:** Identity Providers → Your Signicat Provider
2. **Go to:** Settings → Advanced
3. **Set First Login Flow:** Select your custom "NiN Auto-Link First Broker Login" flow
4. **Save the configuration**

### 4. User Setup

Ensure your local users have the NiN attribute set:

1. **Navigate to:** Users → Select User → Attributes
2. **Add attribute:**
   - Key: `nin`
   - Value: `[user's Norwegian Identity Number]`

Alternatively, users can have their NiN as their username.

## 🔍 How It Works

1. **Broker Login Detection:** The authenticator detects when a user is logging in via a federated IdP
2. **NiN Extraction:** Extracts the NiN from the brokered identity claims
3. **User Lookup:** Searches for a local user with matching NiN (as username or attribute)
4. **Credential Check:** Verifies the local user has no stored password
5. **Auto-Link:** If all conditions are met, automatically links the accounts and proceeds
6. **Fallback:** If conditions aren't met, falls back to normal password verification

## 🧪 Testing

### Unit Tests

Run the test suite:

```bash
mvn test
```

### Integration Testing

1. **Set up test users:**
   - Create a user with NiN attribute but no password
   - Create a user with NiN attribute and password
   - Create a user without NiN attribute

2. **Test scenarios:**
   - Login via Signicat with NiN claim → should auto-link (no password user)
   - Login via Signicat with NiN claim → should prompt for password (password user)
   - Login via Signicat without NiN claim → should prompt for password

## 📋 Sample Identity Provider Configuration

For Signicat OIDC integration, ensure your IdP configuration includes:

```json
{
  "alias": "signicat",
  "providerId": "oidc",
  "enabled": true,
  "config": {
    "authorizationUrl": "https://preprod.signicat.com/oidc/authorize",
    "tokenUrl": "https://preprod.signicat.com/oidc/token",
    "userInfoUrl": "https://preprod.signicat.com/oidc/userinfo",
    "clientId": "your-client-id",
    "clientSecret": "your-client-secret",
    "defaultScope": "openid profile nin",
    "syncMode": "IMPORT"
  }
}
```

**Important:** Ensure the `nin` claim is included in the scope and mapped in the attribute mapper.

## 🔧 Troubleshooting

### Common Issues

1. **Authenticator not appearing in dropdown:**
   - Ensure the JAR is in the correct providers directory
   - Restart Keycloak after adding the JAR
   - Check Keycloak logs for SPI loading errors

2. **NiN not being extracted:**
   - Verify the NiN claim is included in the OIDC scope
   - Check that the claim mapping is configured correctly
   - Review Keycloak logs for claim extraction details

3. **Auto-linking not working:**
   - Verify the local user has no password set
   - Check that the NiN attribute matches exactly
   - Ensure the user is not disabled

### Logging

Enable debug logging for the authenticator:

```bash
# Add to Keycloak startup parameters
-Dkeycloak.log.level=DEBUG
-Dkeycloak.log.category.com.keycloak.nin.autolink=DEBUG
```

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📞 Support

For issues and questions:

- Create an issue in the GitHub repository
- Check the troubleshooting section above
- Review Keycloak documentation for SPI development
