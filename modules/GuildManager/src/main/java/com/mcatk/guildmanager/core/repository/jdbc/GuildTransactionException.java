package com.mcatk.guildmanager.core.repository.jdbc;

public class GuildTransactionException extends IllegalStateException {
    private final boolean outcomeUnknown;
    public GuildTransactionException(String message, Throwable cause, boolean outcomeUnknown) {
        super(message, cause); this.outcomeUnknown = outcomeUnknown;
    }
    public boolean isOutcomeUnknown() { return outcomeUnknown; }
}
