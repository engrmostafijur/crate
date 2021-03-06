/*
 * This file is part of a module with proprietary Enterprise Features.
 *
 * Licensed to Crate.io Inc. ("Crate.io") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 *
 * To use this file, Crate.io must have given you permission to enable and
 * use such Enterprise Features and you must have a valid Enterprise or
 * Subscription Agreement with Crate.io.  If you enable or use the Enterprise
 * Features, you represent and warrant that you have a valid Enterprise or
 * Subscription Agreement with Crate.io.  Your use of the Enterprise Features
 * if governed by the terms and conditions of your Enterprise or Subscription
 * Agreement with Crate.io.
 */

package io.crate.integrationtests;

import io.crate.analyze.user.Privilege;
import io.crate.auth.user.UserManager;
import io.crate.testing.TestingHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;

public class SysPrivilegesIntegrationTest extends BaseUsersIntegrationTest {

    private static final Set<Privilege> PRIVILEGES = new HashSet<>(Arrays.asList(
        new Privilege(Privilege.State.GRANT, Privilege.Type.DQL, Privilege.Clazz.CLUSTER, null, "crate"),
        new Privilege(Privilege.State.GRANT, Privilege.Type.DML, Privilege.Clazz.CLUSTER, null, "crate")));
    private static final List<String> USERNAMES = Arrays.asList("ford", "arthur", "normal");


    @Before
    public void setUpNodesUsersAndPrivileges() throws Exception {
        super.createSessions();

        for (String userName : USERNAMES) {
            executeAsSuperuser("create user " + userName);
        }

        UserManager userManager = internalCluster().getInstance(UserManager.class);
        Long rowCount = userManager.applyPrivileges(USERNAMES, PRIVILEGES).get(5, TimeUnit.SECONDS);
        assertThat(rowCount, is(6L));
    }

    @After
    public void dropUsersAndPrivileges() {
        for (String userName : USERNAMES) {
            executeAsSuperuser("drop user " + userName);
        }
    }

    @Test
    public void testTableColumns() {
        executeAsSuperuser("select column_name, data_type from information_schema.columns" +
                " where table_name='privileges' and table_schema='sys'");
        assertThat(TestingHelpers.printedTable(response.rows()), is("class| text\n" +
                                                                    "grantee| text\n" +
                                                                    "grantor| text\n" +
                                                                    "ident| text\n" +
                                                                    "state| text\n" +
                                                                    "type| text\n"));
    }

    @Test
    public void testListingAsSuperUser() {
        executeAsSuperuser("select * from sys.privileges order by grantee, type");
        assertThat(TestingHelpers.printedTable(response.rows()), is("CLUSTER| arthur| crate| NULL| GRANT| DML\n" +
                                                                    "CLUSTER| arthur| crate| NULL| GRANT| DQL\n" +
                                                                    "CLUSTER| ford| crate| NULL| GRANT| DML\n" +
                                                                    "CLUSTER| ford| crate| NULL| GRANT| DQL\n" +
                                                                    "CLUSTER| normal| crate| NULL| GRANT| DML\n" +
                                                                    "CLUSTER| normal| crate| NULL| GRANT| DQL\n"));
    }

    @Test
    public void testListingAsUserWithPrivilege() {
        executeAsSuperuser("select * from sys.privileges order by grantee, type");
        assertThat(TestingHelpers.printedTable(response.rows()), is("CLUSTER| arthur| crate| NULL| GRANT| DML\n" +
                                                                    "CLUSTER| arthur| crate| NULL| GRANT| DQL\n" +
                                                                    "CLUSTER| ford| crate| NULL| GRANT| DML\n" +
                                                                    "CLUSTER| ford| crate| NULL| GRANT| DQL\n" +
                                                                    "CLUSTER| normal| crate| NULL| GRANT| DML\n" +
                                                                    "CLUSTER| normal| crate| NULL| GRANT| DQL\n"));
    }
}
