package com.streamind.sdk.callbacks;

import com.streamind.sdk.Directive;

/**
 * Directive received callback
 */
@FunctionalInterface
public interface DirectiveCallback {
    /**
     * Called when a directive is received from platform
     *
     * @param directive the received directive
     */
    void onDirectiveReceived(Directive directive);
}
