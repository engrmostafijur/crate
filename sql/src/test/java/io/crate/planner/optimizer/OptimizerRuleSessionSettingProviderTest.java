/*
 * Licensed to Crate under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.  Crate licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial
 * agreement.
 */

package io.crate.planner.optimizer;

import io.crate.action.sql.Option;
import io.crate.action.sql.SessionContext;
import io.crate.analyze.SymbolEvaluator;
import io.crate.auth.user.User;
import io.crate.expression.symbol.Literal;
import io.crate.expression.symbol.Symbol;
import io.crate.metadata.CoordinatorTxnCtx;
import io.crate.metadata.Functions;
import io.crate.metadata.SearchPath;
import io.crate.metadata.settings.SessionSettings;
import io.crate.metadata.settings.session.SessionSetting;
import io.crate.planner.optimizer.rule.MergeFilters;
import io.crate.planner.optimizer.rule.MoveFilterBeneathGroupBy;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static io.crate.testing.TestingHelpers.getFunctions;
import static  io.crate.analyze.SymbolEvaluator.evaluateWithoutParams;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class OptimizerRuleSessionSettingProviderTest {

    private Functions functions = getFunctions();
    private Function<Symbol, Object> eval = x -> evaluateWithoutParams(
        CoordinatorTxnCtx.systemTransactionContext(),
        functions,
        x
    );

    @Test
    public void test_rule_session_settings() {

        var settingsProvider = new OptimizerRuleSessionSettingProvider();
        SessionSetting<?> sessionSetting = settingsProvider.buildRuleSessionSetting(MergeFilters.class);
        assertThat(sessionSetting.name(), is("optimizer_merge_filters"));
        assertThat(sessionSetting.description(), is("Indicates if the optimizer rule MergeFilters is activated."));
        assertThat(sessionSetting.defaultValue(), is("true"));

        var mergefilterSettings = new SessionSettings("userr",
                                                  SearchPath.createSearchPathFrom("dummySchema"),
                                                  true,
                                                  Set.of(MergeFilters.class));

        assertThat("Rule MergeFilters has to excluded", sessionSetting.getValue(mergefilterSettings), is("false"));

        var sessionContext = new SessionContext(Option.NONE, User.of("user"));
        sessionSetting.apply(sessionContext, List.of(Literal.of(false)), eval);

        assertThat(sessionContext.excludedOptimizerRules(), containsInAnyOrder(MergeFilters.class));

        sessionSetting.apply(sessionContext, List.of(Literal.of(true)), eval);
        assertThat(sessionContext.excludedOptimizerRules().isEmpty(), is(true));

    }
}