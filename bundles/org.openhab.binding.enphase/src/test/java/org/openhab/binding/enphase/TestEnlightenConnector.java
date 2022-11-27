/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.enphase;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.openhab.binding.enphase.internal.EntrezJwt;
import org.openhab.binding.enphase.internal.EnvoyConnectionException;

/**
 *
 * Methods to make API calls to to the Enlighten Cloud API.
 *
 * @author Joe Inkenbrandt - Initial contribution
 *
 */
@NonNullByDefault
public class TestEnlightenConnector {

    // private final Logger logger = LoggerFactory.getLogger(TestEnlightenConnector.class);

    @Test
    public void testEnlightenConnector() throws EnvoyConnectionException {
        EntrezJwt entrezJwt = new EntrezJwt(
                "eyJraWQiOiI3ZDEwMDA1ZC03ODk5LTRkMGQtYmNiNC0yNDRmOThlZTE1NmIiLCJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJhdWQiOiIxMjIxMDMwNjI0NjUiLCJpc3MiOiJFbnRyZXoiLCJlbnBoYXNlVXNlciI6Im93bmVyIiwiZXhwIjoxNjY3NzYxMTU2LCJpYXQiOjE2Njc3NTc1NTYsImp0aSI6ImQ0MDIyNWNjLWI3YTQtNGU1Yi1hZTg0LTUxNjIwZmEwZWY5ZiIsInVzZXJuYW1lIjoib3BlbmhhYkBpbmtlbmJyYW5kdC5jb20ifQ.np4ohIYsy6SHRelGkRcrsC4IkNEZ2chMMcVRDMj2n4pZTAc7h5RTTDcl_nGEDTeS0JC-sU2fXweqyZKYilGy1A");
        assertTrue(entrezJwt.getJwt() != null);

        // assertTrue(entrezJwt.retrieveJwt("<your enphase login>", "<your enphase password>", "<Your Site ID>",
        // "<Your Gateway Serial Number>"));
    }
}
