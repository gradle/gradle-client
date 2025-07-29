package org.gradle.client.softwaretype.compose;

import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.internal.plugins.BuildModel;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Nested;
import org.jetbrains.compose.ComposeExtension;
import org.jetbrains.compose.desktop.DesktopExtension;

import javax.inject.Inject;

public interface ComposeBuildModel extends BuildModel {

    ComposeExtension getComposeExtension();

}
