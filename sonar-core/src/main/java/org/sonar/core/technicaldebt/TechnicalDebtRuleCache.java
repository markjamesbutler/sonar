/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.core.technicaldebt;

import com.google.common.collect.Maps;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated since 4.3
 */
@Deprecated
public class TechnicalDebtRuleCache {

  private final RuleFinder ruleFinder;

  private Map<String, Map<String, Rule>> cachedRules;
  private Map<Integer, Rule> cachedRulesId;

  public TechnicalDebtRuleCache(RuleFinder ruleFinder) {
    this.ruleFinder = ruleFinder;
  }

  @CheckForNull
  public Rule getByRuleKey(RuleKey ruleKey) {
    initRules();
    return lookUpRuleInCache(ruleKey.repository(), ruleKey.rule());
  }

  @CheckForNull
  public Rule getByRuleId(Integer ruleId) {
    initRules();
    return cachedRulesId.get(ruleId);
  }

  public boolean exists(Integer ruleId) {
    initRules();
    return getByRuleId(ruleId) != null;
  }

  public boolean exists(RuleKey ruleKey) {
    return getByRuleKey(ruleKey) != null;
  }

  private void initRules(){
    if(cachedRules == null) {
      loadRules();
    }
  }

  private void loadRules() {
    cachedRules = Maps.newHashMap();
    cachedRulesId = Maps.newHashMap();
    Collection<Rule> rules = ruleFinder.findAll(RuleQuery.create());
    for (Rule rule : rules) {
      if(!cachedRules.containsKey(rule.getRepositoryKey())) {
        cachedRules.put(rule.getRepositoryKey(), new HashMap<String, Rule>());
      }
      Map<String, Rule> cachedRepository = cachedRules.get(rule.getRepositoryKey());
      if(!cachedRepository.containsKey(rule.getKey())) {
        cachedRepository.put(rule.getKey(), rule);
        cachedRulesId.put(rule.getId(), rule);
      }
    }
  }

  @CheckForNull
  private Rule lookUpRuleInCache(String repository, String ruleKey) {
    Map<String, Rule> cachedRepository = cachedRules.get(repository);
    if(cachedRepository != null) {
      return cachedRepository.get(ruleKey);
    }
    return null;
  }

}
