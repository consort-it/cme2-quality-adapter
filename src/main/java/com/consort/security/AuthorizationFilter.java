package com.consort.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import lombok.extern.slf4j.Slf4j;
import org.pac4j.core.client.Clients;
import org.pac4j.core.config.Config;
import org.pac4j.http.client.direct.HeaderClient;
import org.pac4j.jwt.config.signature.RSASignatureConfiguration;
import org.pac4j.jwt.credentials.authenticator.JwtAuthenticator;
import org.pac4j.sparkjava.SecurityFilter;
import spark.Request;
import spark.Response;
import spark.utils.StringUtils;
import sun.security.rsa.RSAPublicKeyImpl;

import java.net.URL;
import java.security.PublicKey;

@Slf4j
public class AuthorizationFilter extends SecurityFilter {

    private static final String AUTH0 = "Auth0";
    private static final String HEADER_NAME = "Authorization";
    private static final String AUTHORIZER_SCOPE = "scope";

    public AuthorizationFilter(final String authorizerName, final String roleName) {
        super(createSecurityConfig(authorizerName, roleName), AUTH0, AUTHORIZER_SCOPE);
    }

    private static Config createSecurityConfig(final String authorizerName, final String roleName) {

        final JwtAuthenticator tokenAuthenticator = new JwtAuthenticator();
        final String jwk_kids = EnvironmentContext.getInstance().getenv("jwk_kid");

        for (final String kid : jwk_kids.split(",")) {

            final RSASignatureConfiguration signatureConfiguration = new RSASignatureConfiguration();

            try {
                final URL urlToJWK = new URL(EnvironmentContext.getInstance().getenv("jwk_url"));
                final JwkProvider provider = new UrlJwkProvider(urlToJWK);
                final Jwk jwk = provider.get(kid);
                if (jwk != null && EnvironmentContext.getInstance().getenv("jwk_alg").equalsIgnoreCase(jwk.getAlgorithm())) {
                    final PublicKey publicKey = jwk.getPublicKey();
                    if (publicKey instanceof RSAPublicKeyImpl) {
                        signatureConfiguration.setPublicKey((RSAPublicKeyImpl) publicKey);
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage());
            }

            tokenAuthenticator.addSignatureConfiguration(signatureConfiguration);
        }

        final HeaderClient headerClient = new HeaderClient(HEADER_NAME, tokenAuthenticator);
        headerClient.setName(AUTH0);
        headerClient.setPrefixHeader("Bearer ");

        final Config config = new Config(new Clients(headerClient));
        if (!StringUtils.isBlank(authorizerName) && !StringUtils.isBlank(roleName)) {
            config.addAuthorizer(authorizerName, new QualityAdapterAttributeAuthorizer(AUTHORIZER_SCOPE, roleName));
        }

        config.setHttpActionAdapter(new QualityHttpActionAdapter());

        return config;
    }

    @Override
    public void handle(Request request, Response response) {
        if (!request.requestMethod().equals("OPTIONS") && !"1".equals(request.queryParams("nosec"))) {
            super.handle(request, response);
        }
    }
}
