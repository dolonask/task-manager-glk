package kg.megalab.taskmanager.dto.transfer;

/** Used for reject/return. {@code signature} (Infodocs) is accepted but not verified in this pass. */
public record DecisionRequest(String decisionReason, Signature signature) {

    public record Signature(String provider, String signedPayloadId) {
    }
}
