package uk.gov.hmcts.juror.pnc.check.service;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.juror.pnc.check.config.RuleConfig;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.AllowedDisposalCodesRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.DisposalMustNotEndWithInRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.QualLiteralCheckRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.SentenceLengthRule;
import uk.gov.hmcts.juror.pnc.check.rules.support.AndRule;
import uk.gov.hmcts.juror.pnc.check.rules.support.Rule;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSet;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleValidationResult;
import uk.gov.hmcts.juror.pnc.check.service.contracts.RuleService;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RuleServiceImpl implements RuleService {

    private final Map<RuleSet<?>, List<Rule<?>>> rulesetsMap;
    private final Clock clock;

    @Autowired
    public RuleServiceImpl(RuleConfig ruleConfig, Collection<Rule<?>> ruleList, Clock clock) {
        this.clock = clock;
        this.rulesetsMap = createRuleSetMap(ruleConfig, ruleList);
    }

    final Map<RuleSet<?>, List<Rule<?>>> createRuleSetMap(RuleConfig ruleConfig, Collection<Rule<?>> defaultRules) {
        final List<Rule<?>> combinedRules;
        if (defaultRules == null) {
            combinedRules = new ArrayList<>();
        } else {
            combinedRules = new ArrayList<>(defaultRules);
        }

        combinedRules.addAll(getRulesFromRuleConfig(ruleConfig));
        return combinedRules
            .stream()
            .collect(Collectors.<Rule<?>, RuleSet<?>>groupingBy(Rule::getRuleSet));
    }

    final List<Rule<?>> getRulesFromRuleConfig(RuleConfig ruleConfig) {
        if (ruleConfig.getConditionalRules() == null) {
            return Collections.emptyList();
        }
        List<Rule<?>> rules = new ArrayList<>();
        ruleConfig.getConditionalRules()
            .forEach(conditionalRule -> {
                Rule<?> rule = createRuleFromRuleConfig(conditionalRule);
                rules.add(rule);
            });
        return rules;
    }


    @SuppressWarnings(
        "PMD.AvoidInstantiatingObjectsInLoops"  //Required as loading configuration values
    )
    Rule<?> createRuleFromRuleConfig(RuleConfig.ConditionalRule conditionalRule) {
        if (conditionalRule.getConditions() == null || conditionalRule.getConditions().isEmpty()) {
            log.info("Using AllowedDisposalCodes as no conditions found for rule: " + conditionalRule);
            return new AllowedDisposalCodesRule(conditionalRule.getWhenCodeIsOneOf(), false);
        }
        AndRule disposalAndRule = new AndRule(conditionalRule.getWhenCodeIsOneOf(), false);
        //Note: currently only the disposal ruleset is supported
        for (RuleConfig.ConditionalRule.Condition condition : conditionalRule.getConditions()) {
            int rulesAdded = 0;
            if (condition.getSentenceLength() != null) {
                Map.Entry<Comparator, Map<DateUnit, Integer>> sentenceLength =
                    condition.getSentenceLength().entrySet()
                        .stream().findFirst().orElseThrow();

                Map.Entry<DateUnit, Integer> dateUnit =
                    sentenceLength.getValue().entrySet().stream().findFirst().orElseThrow();

                disposalAndRule.addRule(new SentenceLengthRule(conditionalRule.getWhenCodeIsOneOf(),
                    condition.isFailOnPass(), sentenceLength.getKey(), dateUnit.getKey(), dateUnit.getValue()));
                rulesAdded++;
            }
            if (condition.getDisposalMustNotEndWithIn() != null) {
                Map.Entry<DateUnit, Integer> disposalEndsWithin =
                    condition.getDisposalMustNotEndWithIn().entrySet().stream().findFirst().orElseThrow();
                disposalAndRule.addRule(new DisposalMustNotEndWithInRule(conditionalRule.getWhenCodeIsOneOf(),
                    condition.isFailOnPass(), clock, disposalEndsWithin.getValue(), disposalEndsWithin.getKey()));
                rulesAdded++;
            }
            if (condition.getQualLiteralCheck() != null) {
                disposalAndRule.addRule(new QualLiteralCheckRule(conditionalRule.getWhenCodeIsOneOf(),
                    condition.isFailOnPass(), condition.getQualLiteralCheck().getExpectedValue()));
                rulesAdded++;
            }

            if (rulesAdded == 0) {
                disposalAndRule.addRule(new AllowedDisposalCodesRule(conditionalRule.getWhenCodeIsOneOf(),
                    condition.isFailOnPass()));
            }
        }

        return disposalAndRule;
    }


    @Override
    public <T> RuleValidationResult fireRules(RuleSet<T> ruleSet, T value) {
        return fireRules(ruleSet, Collections.singleton(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> RuleValidationResult fireRules(@NotNull RuleSet<T> ruleSet, Collection<T> values) {
        log.trace("Triggering rules for: " + ruleSet.getSupportedClass());
        final Optional<List<Rule<?>>> rulesOptional = Optional.ofNullable(rulesetsMap.get(ruleSet));
        if (rulesOptional.isEmpty()) {
            throw new InternalServerException("Ruleset for "
                + ruleSet.getSupportedClass().getSimpleName() + " must exist");
        }

        for (T value : values) {
            Optional<Rule<?>> ruleOptional = rulesOptional.get().stream()
                .filter(rule -> !(((Rule<T>) rule).validate(value)))
                .findFirst();
            if (ruleOptional.isPresent()) {
                log.info("Rule failed " + ruleOptional.get().getDescription());
                log.debug("Value: " + value);
                return RuleValidationResult.failed(ruleOptional.get().getDescription());
            }
        }
        return RuleValidationResult.passed();
    }
}
