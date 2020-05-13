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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.CaseFormat;
import io.crate.common.collections.Lists2;
import io.crate.metadata.settings.session.SessionSetting;
import io.crate.metadata.settings.session.SessionSettingProvider;
import io.crate.planner.operators.RewriteInsertFromSubQueryToInsertFromValues;
import io.crate.planner.optimizer.rule.DeduplicateOrder;
import io.crate.planner.optimizer.rule.MergeAggregateAndCollectToCount;
import io.crate.planner.optimizer.rule.MergeFilterAndCollect;
import io.crate.planner.optimizer.rule.MergeFilters;
import io.crate.planner.optimizer.rule.MoveFilterBeneathFetchOrEval;
import io.crate.planner.optimizer.rule.MoveFilterBeneathGroupBy;
import io.crate.planner.optimizer.rule.MoveFilterBeneathHashJoin;
import io.crate.planner.optimizer.rule.MoveFilterBeneathNestedLoop;
import io.crate.planner.optimizer.rule.MoveFilterBeneathOrder;
import io.crate.planner.optimizer.rule.MoveFilterBeneathProjectSet;
import io.crate.planner.optimizer.rule.MoveFilterBeneathRename;
import io.crate.planner.optimizer.rule.MoveFilterBeneathUnion;
import io.crate.planner.optimizer.rule.MoveFilterBeneathWindowAgg;
import io.crate.planner.optimizer.rule.MoveOrderBeneathFetchOrEval;
import io.crate.planner.optimizer.rule.MoveOrderBeneathNestedLoop;
import io.crate.planner.optimizer.rule.MoveOrderBeneathRename;
import io.crate.planner.optimizer.rule.MoveOrderBeneathUnion;
import io.crate.planner.optimizer.rule.RemoveRedundantFetchOrEval;
import io.crate.planner.optimizer.rule.RewriteCollectToGet;
import io.crate.planner.optimizer.rule.RewriteFilterOnOuterJoinToInnerJoin;
import io.crate.planner.optimizer.rule.RewriteGroupByKeysLimitToTopNDistinct;
import io.crate.planner.optimizer.rule.RewriteToQueryThenFetch;
import io.crate.types.DataTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LoadedRules implements SessionSettingProvider, RuleProvider {

    private static final String OPTIMIZER_RULE = "optimizer_";

    private static final List<Rule<?>> rules = List.of(
        new RemoveRedundantFetchOrEval(),
        new MergeAggregateAndCollectToCount(),
        new MergeFilters(),
        new MoveFilterBeneathRename(),
        new MoveFilterBeneathFetchOrEval(),
        new MoveFilterBeneathOrder(),
        new MoveFilterBeneathProjectSet(),
        new MoveFilterBeneathHashJoin(),
        new MoveFilterBeneathNestedLoop(),
        new MoveFilterBeneathUnion(),
        new MoveFilterBeneathGroupBy(),
        new MoveFilterBeneathWindowAgg(),
        new MergeFilterAndCollect(),
        new RewriteFilterOnOuterJoinToInnerJoin(),
        new MoveOrderBeneathUnion(),
        new MoveOrderBeneathNestedLoop(),
        new MoveOrderBeneathFetchOrEval(),
        new MoveOrderBeneathRename(),
        new DeduplicateOrder(),
        new RewriteCollectToGet(),
        new RewriteGroupByKeysLimitToTopNDistinct(),
        new RewriteInsertFromSubQueryToInsertFromValues(),
        new RewriteToQueryThenFetch()
    );

    @Override
    public List<SessionSetting<?>> sessionSettings() {
        return Lists2.map(rules, this::buildRuleSessionSetting);
    }

    @VisibleForTesting
    SessionSetting<?> buildRuleSessionSetting(Rule<?> rule) {
        Class<? extends Rule> clazz = rule.getClass();
        var simpleName = clazz.getSimpleName();
        var optimizerRuleName = OPTIMIZER_RULE + CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, simpleName);
        return new SessionSetting<>(
            optimizerRuleName,
            objects -> {
            },
            objects -> DataTypes.BOOLEAN.value(objects[0]),
            (sessionContext, activateRule) -> rule.setEnabled(activateRule),
            s -> String.valueOf(rule.isEnabled()),
            () -> String.valueOf(true),
            String.format(Locale.ENGLISH, "Indicates if the optimizer rule %s is activated.", simpleName),
            DataTypes.BOOLEAN.getName()
        );
    }

    @Override
    public final List<Rule<?>> getRules(List<Class<? extends Rule<?>>> includedRules) {
        if (includedRules.isEmpty()) {
            return rules;
        }
        var includes = Set.of(includedRules);
        var result = new ArrayList<Rule<?>>(includes.size());
        for (var rule : rules) {
            if (includes.contains(rule.getClass())) {
                result.add(rule);
            }
        }
        return result;
    }
}
