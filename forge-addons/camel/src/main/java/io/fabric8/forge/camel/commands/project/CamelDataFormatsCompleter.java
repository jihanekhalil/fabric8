/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package io.fabric8.forge.camel.commands.project;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.camel.catalog.CamelCatalog;
import org.apache.camel.catalog.DefaultCamelCatalog;
import org.apache.camel.catalog.JSonSchemaHelper;
import org.jboss.forge.addon.dependencies.Dependency;
import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;

import static io.fabric8.forge.camel.commands.project.CamelCatalogHelper.dataFormatsFromArtifact;
import static io.fabric8.forge.camel.commands.project.CamelProjectHelper.findCamelArtifacts;

public class CamelDataFormatsCompleter implements UICompleter<String> {

    private Project project;

    public CamelDataFormatsCompleter(Project project) {
        this.project = project;
    }

    @Override
    public Iterable<String> getCompletionProposals(UIContext context, InputComponent input, String value) {
        List<String> answer = new ArrayList<String>();
        // find the version of Apache Camel we use

        // need to find camel-core so we known the camel version
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return null;
        }

        // find all available dataformat names
        CamelCatalog catalog = new DefaultCamelCatalog();
        List<String> names = catalog.findDataFormatNames();

        // filter non matching names first
        List<String> filtered = new ArrayList<String>();
        for (String name : names) {
            if (value == null || name.startsWith(value)) {
                filtered.add(name);
            }
        }

        // filter names which are already on the classpath
        for (String name : filtered) {
            String json = catalog.dataFormatJSonSchema(name);
            String artifactId = findArtifactId(json);

            // skip if we already have the dependency
            boolean already = false;
            if (artifactId != null) {
                already = CamelProjectHelper.hasDependency(project, "org.apache.camel", artifactId);
            }
            if (!already) {
                answer.add(name);
            }
        }

        return answer;
    }

    public Iterable<String> getValueChoices() {
        // need to find camel-core so we known the camel version
        Dependency core = CamelProjectHelper.findCamelCoreDependency(project);
        if (core == null) {
            return null;
        }

        // find all available component names
        CamelCatalog catalog = new DefaultCamelCatalog();
        List<String> names = catalog.findDataFormatNames();

        // filter out existing dataformats we already have
        Set<Dependency> artifacts = findCamelArtifacts(project);
        for (Dependency dep : artifacts) {
            Set<String> languages = dataFormatsFromArtifact(dep.getCoordinate().getArtifactId());
            names.removeAll(languages);
        }

        return names;
    }

    private static String findArtifactId(String json) {
        List<Map<String, String>> data = JSonSchemaHelper.parseJsonSchema("dataformat", json, false);
        for (Map<String, String> row : data) {
            if (row.get("artifactId") != null) {
                return row.get("artifactId");
            }
        }
        return null;
    }

}
