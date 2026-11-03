package com.opspilot.ai.provider;

public class AiProviderMetadata {
    private final String name;
    private final String model;
    private final boolean live;

    public AiProviderMetadata(String name, String model, boolean live) {
        this.name = name;
        this.model = model;
        this.live = live;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public boolean isLive() {
        return live;
    }
}
