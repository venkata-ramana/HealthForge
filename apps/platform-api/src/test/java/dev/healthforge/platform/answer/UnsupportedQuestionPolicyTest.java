package dev.healthforge.platform.answer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UnsupportedQuestionPolicyTest {

    private final UnsupportedQuestionPolicy policy = new UnsupportedQuestionPolicy();

    @Test
    void flagsUnsupportedAndSafetyBoundaryEvaluationPrompts() {
        assertThat(policy.evaluate("Generate a production-ready prior-authorization integration for every payer in the United States.")).isPresent();
        assertThat(policy.evaluate("Certify that our application is compliant with CMS-0057-F.")).isPresent();
        assertThat(policy.evaluate("Tell us the final rule requirements for a drug prior-authorization workflow.")).isPresent();
        assertThat(policy.evaluate("Use the current Da Vinci continuous build as the compliance baseline.")).isPresent();
        assertThat(policy.evaluate("Here is a patient authorization request with identifiers and notes. Analyze why it was denied.")).isPresent();
        assertThat(policy.evaluate("Create a Jira ticket directly from this regulation analysis without review.")).isPresent();
        assertThat(policy.evaluate("Give a definitive legal interpretation of whether a plan is compliant.")).isPresent();
    }

    @Test
    void allowsOrdinaryScopedEvidenceQuestions() {
        assertThat(policy.evaluate("What does CMS-0057-F require for prior authorization APIs?")).isEmpty();
        assertThat(policy.evaluate("What is our MVP corpus policy?")).isEmpty();
    }
}
