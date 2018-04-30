package com.consort.security;

import org.pac4j.core.authorization.authorizer.RequireAnyAttributeAuthorizer;

public class QualityAdapterAttributeAuthorizer extends RequireAnyAttributeAuthorizer {

    public QualityAdapterAttributeAuthorizer(final String attribute, final String valueToMatch) {
        super(valueToMatch);
        setElements(attribute);
    }
}
