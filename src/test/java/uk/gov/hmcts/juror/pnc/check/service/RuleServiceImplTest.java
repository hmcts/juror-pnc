package uk.gov.hmcts.juror.pnc.check.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.juror.pnc.check.config.RuleConfig;
import uk.gov.hmcts.juror.pnc.check.model.pnc.DisposalDto;
import uk.gov.hmcts.juror.pnc.check.rules.Comparator;
import uk.gov.hmcts.juror.pnc.check.rules.DateUnit;
import uk.gov.hmcts.juror.pnc.check.rules.NotOnBailRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.AllowedDisposalCodesRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.DisposalMustNotEndWithInRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.QualLiteralCheckRule;
import uk.gov.hmcts.juror.pnc.check.rules.disposal.SentenceLengthRule;
import uk.gov.hmcts.juror.pnc.check.rules.support.AndRule;
import uk.gov.hmcts.juror.pnc.check.rules.support.DisposalRule;
import uk.gov.hmcts.juror.pnc.check.rules.support.Rule;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSet;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleSets;
import uk.gov.hmcts.juror.pnc.check.rules.support.RuleValidationResult;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestRule;
import uk.gov.hmcts.juror.pnc.check.testsupport.TestUtil;
import uk.gov.hmcts.juror.standard.service.exceptions.InternalServerException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings({
    "unchecked",
    "PMD.LawOfDemeter",
    "PMD.ExcessiveImports",
    "PMD.AvoidDuplicateLiterals",
    "PMD.TooManyMethods"
})
class RuleServiceImplTest {


    private RuleServiceImpl ruleService;
    private Clock clock;
    private RuleConfig ruleConfig;

    @BeforeEach
    void beforeEach() {
        this.ruleConfig = new RuleConfig();
        this.clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
    }

    private void setupRuleService(Collection<Rule<?>> ruleList) {
        this.ruleService = spy(new RuleServiceImpl(ruleConfig, ruleList, clock));
    }

    private void setupRuleService() {
        setupRuleService(Collections.emptyList());
    }


    @Nested
    @DisplayName("Map<RuleSet<?>, List<Rule<?>>> "
        + "createRuleSetMap(RuleConfig ruleConfig, Collection<Rule<?>> defaultRules)")
    class CreateRuleSetMap {
        @Test
        @DisplayName("Has both default and rule config rules")
        void defaultAndRuleConfigRules() {
            setupRuleService();
            RuleConfig newRuleConfig = new RuleConfig();
            List<Rule<?>> defaultRules = List.of(
                new TestRule(true),
                new TestRule(false),
                new NotOnBailRule()
            );

            List<Rule<?>> rulesFromRuleConfig = List.of(
                new TestRule(true),
                new NotOnBailRule());
            when(ruleService.getRulesFromRuleConfig(ruleConfig)).thenReturn(rulesFromRuleConfig);

            Map<RuleSet<?>, List<Rule<?>>> ruleSetMap = ruleService
                .createRuleSetMap(newRuleConfig, defaultRules);

            List<Rule<?>> testRules = ruleSetMap.get(TestRule.RULE_SET);
            List<Rule<?>> notOnBailRules = ruleSetMap.get(RuleSets.PERSON_RULE_SET);

            assertEquals(2, ruleSetMap.size(), "Ruleset map should only contain 2 types of rule sets");
            assertEquals(3, testRules.size(), "Test rules size must match number of test rules");
            assertEquals(2, notOnBailRules.size(), "NotOnBail rules size must match number of test rules");

            assertThat("Test rules must match", testRules, hasItems(defaultRules.get(0), defaultRules.get(1),
                rulesFromRuleConfig.get(0)));
            assertThat("NotOnBail rules must match", notOnBailRules, hasItems(defaultRules.get(2),
                rulesFromRuleConfig.get(1)));
        }

        @Test
        @DisplayName("Has only default rules")
        void defaultRules() {
            setupRuleService();
            RuleConfig newRuleConfig = new RuleConfig();
            List<Rule<?>> defaultRules = List.of(
                new TestRule(true),
                new TestRule(false),
                new NotOnBailRule()
            );

            when(ruleService.getRulesFromRuleConfig(ruleConfig)).thenReturn(Collections.emptyList());

            Map<RuleSet<?>, List<Rule<?>>> ruleSetMap = ruleService
                .createRuleSetMap(newRuleConfig, defaultRules);

            List<Rule<?>> testRules = ruleSetMap.get(TestRule.RULE_SET);
            List<Rule<?>> notOnBailRules = ruleSetMap.get(RuleSets.PERSON_RULE_SET);

            assertEquals(2, ruleSetMap.size(), "Ruleset map should only contain 2 types of rule sets");
            assertEquals(2, testRules.size(), "Test rules size must match number of test rules");
            assertEquals(1, notOnBailRules.size(), "NotOnBail rules size must match number of test rules");

            assertThat("Test rules must match", testRules, hasItems(defaultRules.get(0), defaultRules.get(1)));
            assertThat("NotOnBail rules must match", notOnBailRules, hasItems(defaultRules.get(2)));
        }

        @Test
        @DisplayName("Has rule config rules")
        void ruleConfigRules() {
            setupRuleService();
            RuleConfig newRuleConfig = new RuleConfig();

            List<Rule<?>> rulesFromRuleConfig = List.of(
                new TestRule(true),
                new NotOnBailRule());
            when(ruleService.getRulesFromRuleConfig(ruleConfig)).thenReturn(rulesFromRuleConfig);

            Map<RuleSet<?>, List<Rule<?>>> ruleSetMap = ruleService
                .createRuleSetMap(newRuleConfig, null);

            List<Rule<?>> testRules = ruleSetMap.get(TestRule.RULE_SET);
            List<Rule<?>> notOnBailRules = ruleSetMap.get(RuleSets.PERSON_RULE_SET);

            assertEquals(2, ruleSetMap.size(), "Ruleset map should only contain 2 types of rule sets");
            assertEquals(1, testRules.size(), "Test rules size must match number of test rules");
            assertEquals(1, notOnBailRules.size(), "NotOnBail rules size must match number of test rules");

            assertThat("Test rules must match", testRules, hasItems(rulesFromRuleConfig.get(0)));
            assertThat("NotOnBail rules must match", notOnBailRules, hasItems(rulesFromRuleConfig.get(1)));
        }

        @Test
        @DisplayName("Empty rules")
        void emptyRules() {
            setupRuleService();
            RuleConfig newRuleConfig = new RuleConfig();

            when(ruleService.getRulesFromRuleConfig(ruleConfig)).thenReturn(Collections.emptyList());

            Map<RuleSet<?>, List<Rule<?>>> ruleSetMap = ruleService
                .createRuleSetMap(newRuleConfig, Collections.emptyList());

            assertEquals(0, ruleSetMap.size(), "Ruleset map should have no rule sets");
        }
    }

    @Nested
    @DisplayName("List<Rule<?>> getRulesFromRuleConfig(RuleConfig ruleConfig)")
    class GetRulesFromRuleConfig {

        @Test
        @DisplayName("No conditional rules")
        void noConditionalRules() {
            setupRuleService();
            ruleConfig.setConditionalRules(null);
            List<Rule<?>> rules = ruleService.getRulesFromRuleConfig(ruleConfig);
            assertEquals(0, rules.size(), "Rules should default to an empty list");
        }

        @Test
        @DisplayName("Empty conditional rules")
        void emptyConditionalRule() {
            setupRuleService();
            ruleConfig.setConditionalRules(Collections.emptyList());
            List<Rule<?>> rules = ruleService.getRulesFromRuleConfig(ruleConfig);
            assertEquals(0, rules.size(), "Rules should default to an empty list");
        }

        @Test
        @DisplayName("Single conditional rules")
        void singleConditionalRule() {
            setupRuleService();
            List<RuleConfig.ConditionalRule> conditionalRules = List.of(
                RuleConfig.ConditionalRule.builder().conditions(Collections.emptyList()).build()
            );

            ruleConfig.setConditionalRules(conditionalRules);

            List<Rule<?>> rules = ruleService.getRulesFromRuleConfig(ruleConfig);

            assertEquals(1, rules.size(), "Rules size should match config");
        }

        @Test
        @DisplayName("Multiple conditional rules")
        void multipleConditionalRule() {
            setupRuleService();
            List<RuleConfig.ConditionalRule> conditionalRules = List.of(
                RuleConfig.ConditionalRule.builder().conditions(Collections.emptyList()).build(),
                RuleConfig.ConditionalRule.builder().conditions(Collections.emptyList()).build(),
                RuleConfig.ConditionalRule.builder().conditions(Collections.emptyList()).build()
            );

            ruleConfig.setConditionalRules(conditionalRules);

            List<Rule<?>> rules = ruleService.getRulesFromRuleConfig(ruleConfig);
            assertEquals(3, rules.size(), "Rules size should match config");

        }
    }

    @Nested
    @DisplayName("Rule<?> createRuleFromRuleConfig(RuleConfig.ConditionalRule conditionalRule)")
    class CreateRuleFromRuleConfig {
        private static final Set<Integer> WHEN_CODE_IS_ONE_OF = Set.of(1, 2, 3, 4, 5);

        @BeforeEach
        void beforeEach() {
            setupRuleService();
        }

        @Test
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        @DisplayName("Conditions are null")
        void nullConditions() {
            Rule<?> rule = ruleService.createRuleFromRuleConfig(
                RuleConfig.ConditionalRule.builder()
                    .conditions(null)
                    .whenCodeIsOneOf(WHEN_CODE_IS_ONE_OF)
                    .build());
            validateAllowedDisposalCodesRule(rule, null);
        }

        @Test
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        @DisplayName("Conditions are empty")
        void emptyConditions() {
            Rule<?> rule = ruleService.createRuleFromRuleConfig(
                RuleConfig.ConditionalRule.builder()
                    .conditions(Collections.emptyList())
                    .whenCodeIsOneOf(WHEN_CODE_IS_ONE_OF)
                    .build());
            validateAllowedDisposalCodesRule(rule, null);

        }

        @ParameterizedTest(name = "Single Sentence Length rule: failOnPass: {0}")
        @ValueSource(booleans = {true, false})
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        void singleSentenceLengthRule(boolean failOnPass) {
            RuleConfig.ConditionalRule.Condition condition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .sentenceLength(Map.of(Comparator.GREATER_THAN, Map.of(DateUnit.YEARS, 1)))
                    .failOnPass(failOnPass)
                    .build();
            Rule<?> rule = triggerWithCondition(condition);
            AndRule andRule = validateAndRule(rule, SentenceLengthRule.class);
            validateSentenceLengthRule(andRule.getRules().get(0), condition, Comparator.GREATER_THAN, 1);
        }

        @ParameterizedTest(name = "Single disposal must not end within rule: failOnPass: {0}")
        @ValueSource(booleans = {true, false})
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        void singleDisposalMustNotEndWithInRule(boolean failOnPass) {
            RuleConfig.ConditionalRule.Condition condition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .disposalMustNotEndWithIn(Map.of(DateUnit.YEARS, 2))
                    .failOnPass(failOnPass)
                    .build();
            Rule<?> rule = triggerWithCondition(condition);
            AndRule andRule = validateAndRule(rule, DisposalMustNotEndWithInRule.class);
            validateDisposalMustNotEndWithInRule(andRule.getRules().get(0), condition, DateUnit.YEARS, 2);
        }


        @ParameterizedTest(name = "Single Qual Literal check rule: failOnPass: {0}")
        @ValueSource(booleans = {true, false})
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        void singleQualLiteralCheckRule(boolean failOnPass) {
            RuleConfig.ConditionalRule.Condition condition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .qualLiteralCheck(new RuleConfig.ConditionalRule.Condition.QualLiteralCheck("S"))
                    .failOnPass(failOnPass)
                    .build();
            Rule<?> rule = triggerWithCondition(condition);
            AndRule andRule = validateAndRule(rule, QualLiteralCheckRule.class);
            validateQualLiteralCheckRule(andRule.getRules().get(0), condition, "S");
        }


        @ParameterizedTest(name = "Single Allowed Disposal Codes: failOnPass: {0}")
        @ValueSource(booleans = {true, false})
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        void singleAllowedDisposalCodesRule(boolean failOnPass) {
            RuleConfig.ConditionalRule.Condition condition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .failOnPass(failOnPass)
                    .build();
            Rule<?> rule = triggerWithCondition(condition);
            AndRule andRule = validateAndRule(rule, AllowedDisposalCodesRule.class);
            validateAllowedDisposalCodesRule(andRule.getRules().get(0), condition);
        }


        @Test
        @DisplayName("Multiple Rules")
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        void multipleRules() {
            RuleConfig.ConditionalRule.Condition condition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .sentenceLength(Map.of(Comparator.LESS_THAN, Map.of (DateUnit.YEARS,4)))
                    .disposalMustNotEndWithIn(Map.of(DateUnit.YEARS, 5))
                    .qualLiteralCheck(new RuleConfig.ConditionalRule.Condition.QualLiteralCheck("H"))
                    .failOnPass(false)
                    .build();


            Rule<?> rule = ruleService.createRuleFromRuleConfig(
                RuleConfig.ConditionalRule.builder()
                    .conditions(List.of(condition))
                    .whenCodeIsOneOf(WHEN_CODE_IS_ONE_OF)
                    .build());
            AndRule andRule = validateAndRule(rule, SentenceLengthRule.class,
                DisposalMustNotEndWithInRule.class,
                QualLiteralCheckRule.class);

            validateSentenceLengthRule(andRule.getRules().get(0), condition,
                Comparator.LESS_THAN, 4);
            validateDisposalMustNotEndWithInRule(andRule.getRules().get(1), condition,
                DateUnit.YEARS, 5);
            validateQualLiteralCheckRule(andRule.getRules().get(2), condition,
                "H");
        }

        @Test
        @DisplayName("Multiple conditions")
        //False positive assert is done via internal method
        @SuppressWarnings("PMD.JUnitTestsShouldIncludeAssert")
        void multipleConditions() {
            RuleConfig.ConditionalRule.Condition sentenceLengthCondition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .sentenceLength(Map.of(Comparator.LESS_THAN, Map.of (DateUnit.YEARS,4)))
                    .failOnPass(false)
                    .build();

            RuleConfig.ConditionalRule.Condition disposalMustNotEndWithInCondition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .disposalMustNotEndWithIn(Map.of(DateUnit.YEARS, 5))
                    .failOnPass(true)
                    .build();

            RuleConfig.ConditionalRule.Condition qualLiteralCheckCondition =
                RuleConfig.ConditionalRule.Condition.builder()
                    .qualLiteralCheck(new RuleConfig.ConditionalRule.Condition.QualLiteralCheck("H"))
                    .failOnPass(false)
                    .build();

            Rule<?> rule = ruleService.createRuleFromRuleConfig(
                RuleConfig.ConditionalRule.builder()
                    .conditions(List.of(
                        sentenceLengthCondition,
                        disposalMustNotEndWithInCondition,
                        qualLiteralCheckCondition))
                    .whenCodeIsOneOf(WHEN_CODE_IS_ONE_OF)
                    .build());
            AndRule andRule = validateAndRule(rule, SentenceLengthRule.class,
                DisposalMustNotEndWithInRule.class,
                QualLiteralCheckRule.class);

            validateSentenceLengthRule(andRule.getRules().get(0), sentenceLengthCondition,
                Comparator.LESS_THAN, 4);
            validateDisposalMustNotEndWithInRule(andRule.getRules().get(1), disposalMustNotEndWithInCondition,
                DateUnit.YEARS, 5);
            validateQualLiteralCheckRule(andRule.getRules().get(2), qualLiteralCheckCondition,
                "H");
        }


        private Rule<?> triggerWithCondition(RuleConfig.ConditionalRule.Condition condition) {
            return ruleService.createRuleFromRuleConfig(
                RuleConfig.ConditionalRule.builder()
                    .conditions(List.of(condition))
                    .whenCodeIsOneOf(WHEN_CODE_IS_ONE_OF)
                    .build());
        }

        private AndRule validateAndRule(Rule<?> rule, Class<? extends Rule<DisposalDto>>... expectedRules) {
            assertNotNull(rule, "Rule should not be null");
            assertInstanceOf(AndRule.class, rule,
                "Rule should be AndRule");
            AndRule andRule = (AndRule) rule;
            List<Rule<DisposalDto>> rules = andRule.getRules();

            assertEquals(expectedRules.length, rules.size(), "Number of rules should match");
            int index = 0;
            for (Class<? extends Rule<DisposalDto>> expectedClass : expectedRules) {
                Rule<DisposalDto> locatedRule = rules.get(index++);
                assertInstanceOf(expectedClass, locatedRule,
                    "Class type should match expected");
            }
            return andRule;
        }

        private void validateSentenceLengthRule(Rule<?> rule, RuleConfig.ConditionalRule.Condition condition,
                                                Comparator comparator, int value) {
            assertNotNull(rule, "Rule should not be null");
            assertInstanceOf(SentenceLengthRule.class, rule,
                "Rule should be SentenceLengthRule");

            SentenceLengthRule sentenceLengthRule = (SentenceLengthRule) rule;
            validateDisposalParams(sentenceLengthRule, condition.isFailOnPass());

            assertEquals(comparator, sentenceLengthRule.getComparator(),
                "Comparator should match");
            assertEquals(value, sentenceLengthRule.getValue(),
                "Value should match");
        }

        private void validateDisposalMustNotEndWithInRule(Rule<?> rule, RuleConfig.ConditionalRule.Condition condition,
                                                          DateUnit dateUnit, int value) {
            assertNotNull(rule, "Rule should not be null");
            assertInstanceOf(DisposalMustNotEndWithInRule.class, rule,
                "Rule should be SentenceLengthRule");

            DisposalMustNotEndWithInRule disposalMustNotEndWithInRule = (DisposalMustNotEndWithInRule) rule;
            validateDisposalParams(disposalMustNotEndWithInRule, condition.isFailOnPass());

            assertEquals(clock, disposalMustNotEndWithInRule.getClock(), "Clock must match");
            assertEquals(dateUnit, disposalMustNotEndWithInRule.getDateUnit(), "Date Unit must match");
            assertEquals(value, disposalMustNotEndWithInRule.getValue(), "Value must match");
        }

        private void validateQualLiteralCheckRule(Rule<?> rule,
                                                  RuleConfig.ConditionalRule.Condition condition,
                                                  String expectedValue) {

            assertNotNull(rule, "Rule should not be null");
            assertInstanceOf(QualLiteralCheckRule.class, rule,
                "Rule should be QualLiteralCheckRule");

            QualLiteralCheckRule qualLiteralCheckRule = (QualLiteralCheckRule) rule;
            validateDisposalParams(qualLiteralCheckRule, condition.isFailOnPass());

            assertEquals(expectedValue, qualLiteralCheckRule.getExpectedValue(),
                "Expected value should match");
        }

        private void validateAllowedDisposalCodesRule(Rule<?> rule,
                                                      RuleConfig.ConditionalRule.Condition condition) {
            assertNotNull(rule, "Rule should not be null");
            assertInstanceOf(AllowedDisposalCodesRule.class, rule,
                "Rule should be AllowedDisposalCodesRule");

            AllowedDisposalCodesRule allowedDisposalCodesRule = (AllowedDisposalCodesRule) rule;
            validateDisposalParams(allowedDisposalCodesRule, condition != null && condition.isFailOnPass());

            Set<Integer> allowedCodes = allowedDisposalCodesRule.getAllowedCodes();
            TestUtil.isUnmodifiable(allowedCodes);
            assertEquals(WHEN_CODE_IS_ONE_OF.size(), allowedCodes.size(), "Allowed codes size should match");
            assertThat("Allowed codes should match",
                allowedCodes, hasItems(WHEN_CODE_IS_ONE_OF.toArray(new Integer[0])));
        }

        private void validateDisposalParams(DisposalRule disposalRule, boolean failOnPass) {
            assertNotNull(disposalRule, "Rule should not be null");
            assertEquals(failOnPass, disposalRule.isFailOnPass(),
                "Fail on pass should match");

            Set<Integer> supportedCodes = disposalRule.getSupportedCodes();
            assertNotNull(supportedCodes, "Support code should not be null");
            if (disposalRule instanceof AllowedDisposalCodesRule) {
                assertEquals(0, supportedCodes.size(), "Support code should be emepty for AllowedDisposalCodesRule");
            } else {
                TestUtil.isUnmodifiable(supportedCodes);
                assertEquals(WHEN_CODE_IS_ONE_OF.size(), supportedCodes.size(), "Supported codes size should match");
                assertThat("Supported codes should match",
                    supportedCodes, hasItems(WHEN_CODE_IS_ONE_OF.toArray(new Integer[0])));
            }
        }
    }

    @Test
    void fireRulesSingle() {
        setupRuleService();
        RuleValidationResult expectedResult = RuleValidationResult.passed();
        doReturn(expectedResult)
            .when(ruleService).fireRules(eq(RuleSets.DISPOSAL_RULE_SET), anyCollection());

        DisposalDto disposalDto = new DisposalDto();

        RuleValidationResult result = ruleService.fireRules(RuleSets.DISPOSAL_RULE_SET, disposalDto);
        assertEquals(expectedResult, result, "Results should match");

        verify(ruleService, times(1)).fireRules(RuleSets.DISPOSAL_RULE_SET, Collections.singleton(disposalDto));
    }

    @Nested
    @DisplayName("public <T> RuleValidationResult fireRules(RuleSet<T> ruleSet, Collection<T> values)")
    class FireRulesCollection {

        @Test
        @DisplayName("Rule set does not exist")
        void ruleSetDoesNotExist() {
            setupRuleService();
            InternalServerException exception =
                assertThrows(InternalServerException.class,
                    () -> ruleService.fireRules(RuleSets.DISPOSAL_RULE_SET, Set.of(new DisposalDto())));
            assertEquals("Ruleset for DisposalDto must exist", exception.getMessage(),
                "Messages must match");
        }

        @Test
        @DisplayName("All rules pass")
        void allRulesPass() {
            Rule<Boolean> testRule = new TestRule(false);
            Rule<Boolean> testRule2 = new TestRule(false);
            Rule<Boolean> testRule3 = new TestRule(false);

            setupRuleService(Set.of(testRule, testRule2, testRule3));

            RuleValidationResult result =
                ruleService.fireRules(TestRule.RULE_SET, List.of(true, true, true));

            assertTrue(result.isPassed(), "Result should pass");
            assertNull(result.getMessage(), "Result message should be null");
        }

        @Test
        @DisplayName("One rule fail")
        void oneRuleFails() {
            Rule<Boolean> testRule = new TestRule(false);
            Rule<Boolean> testRule2 = new TestRule(false);
            Rule<Boolean> testRule3 = new TestRule(false);

            setupRuleService(Set.of(testRule, testRule2, testRule3));

            RuleValidationResult result =
                ruleService.fireRules(TestRule.RULE_SET, List.of(true, false, true));

            assertFalse(result.isPassed(), "Result should fail");
            assertEquals("MyDescription", result.getMessage(),
                "Message should match");
        }
    }
}
