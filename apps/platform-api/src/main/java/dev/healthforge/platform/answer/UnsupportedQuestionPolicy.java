package dev.healthforge.platform.answer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
public class UnsupportedQuestionPolicy {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "no_phi_boundary",
                    "The local MVP does not accept patient-identifiable requests or notes. Use only de-identified, synthetic, or otherwise non-sensitive material.",
                    "Reframe the question with synthetic or de-identified content before requesting evidence review.",
                    List.of("patient authorization request", "identifiers and notes")
            ),
            new Rule(
                    "direct_writeback_out_of_scope",
                    "The local MVP does not perform direct workflow writeback or external ticket creation without human review.",
                    "Ask for a proposed work item or reviewable Brief that a human can approve before entering another system.",
                    List.of("create a jira ticket", "without review")
            ),
            new Rule(
                    "compliance_certification_out_of_scope",
                    "The local MVP cannot certify legal, regulatory, or compliance status from retrieved excerpts alone.",
                    "Request a cited gap analysis or engineering-impact review for a defined context and qualified reviewer.",
                    List.of("certify", "compliant with")
            ),
            new Rule(
                    "definitive_legal_interpretation_out_of_scope",
                    "The local MVP does not issue definitive legal or compliance determinations.",
                    "Request a cited engineering-impact analysis and route the question to qualified legal or compliance review.",
                    List.of("definitive legal interpretation")
            ),
            new Rule(
                    "universal_production_claim_out_of_scope",
                    "The local MVP cannot support universal or production-ready implementation claims across every payer context.",
                    "Narrow the request to a specific payer, workflow, or standards question and keep a human reviewer in the loop.",
                    List.of("production-ready", "every payer")
            ),
            new Rule(
                    "drug_workflow_out_of_scope",
                    "The current MVP corpus does not support drug prior-authorization workflow analysis.",
                    "Limit the request to the covered non-drug prior-authorization scope or extend the corpus through review first.",
                    List.of("drug prior-authorization")
            ),
            new Rule(
                    "continuous_build_not_allowed_as_baseline",
                    "The local MVP cannot use an unpinned continuous build as a compliance or standards baseline.",
                    "Use a published, pinned specification or implementation guide version instead.",
                    List.of("continuous build", "compliance baseline")
            )
    );

    public Optional<UnsupportedDecision> evaluate(String question) {
        var normalizedQuestion = normalize(question);
        return RULES.stream()
                .filter(rule -> rule.matches(normalizedQuestion))
                .findFirst()
                .map(rule -> new UnsupportedDecision(rule.id(), rule.limitation(), rule.safeNextStep()));
    }

    private String normalize(String question) {
        return question == null ? "" : question.toLowerCase(Locale.ROOT);
    }

    private record Rule(String id, String limitation, String safeNextStep, List<String> requiredPhrases) {
        private boolean matches(String normalizedQuestion) {
            return requiredPhrases.stream().allMatch(normalizedQuestion::contains);
        }
    }

    public record UnsupportedDecision(String ruleId, String limitation, String safeNextStep) {
    }
}
