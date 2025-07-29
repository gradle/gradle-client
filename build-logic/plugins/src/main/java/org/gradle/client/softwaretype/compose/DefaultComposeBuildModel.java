package org.gradle.client.softwaretype.compose;

import org.jetbrains.compose.ComposeExtension;

public class DefaultComposeBuildModel implements ComposeBuildModel {
    private ComposeExtension composeExtension;

    @Override
    public ComposeExtension getComposeExtension() {
        return composeExtension;
    }

    public void setComposeExtension(ComposeExtension composeExtension) {
        this.composeExtension = composeExtension;
    }
}
