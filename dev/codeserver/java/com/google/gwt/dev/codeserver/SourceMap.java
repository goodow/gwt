/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.dev.codeserver;

import com.google.gwt.dev.json.JsonArray;
import com.google.gwt.dev.json.JsonException;
import com.google.gwt.dev.json.JsonObject;
import com.google.gwt.dev.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * In-memory representation of a SourceMap.
 *
 * @author skybrian@google.com (Brian Slesinsky)
 */
class SourceMap {
  private final JsonObject json;

  /** @see #load */
  private SourceMap(JsonObject json) {
    this.json = json;
  }

  static SourceMap load(File file) {
    String sourceMapJson = Util.readFileAsString(file);

    JsonObject json;
    try {
      json = JsonObject.parse(new StringReader(sourceMapJson));
    } catch (JsonException e) {
      throw new RuntimeException("can't parse sourcemap as json", e);
    } catch (IOException e) {
      throw new RuntimeException("can't parse sourcemap as json", e);
    }

    return new SourceMap(json);
  }

  /**
   * Adds the given prefix to each source filename in the source map.
   */
  void addPrefixToEachSourceFile(String serverPrefix) {
    JsonArray sources = (JsonArray) json.get("sources");
    JsonArray newSources = new JsonArray();
    for (int i = 0; i < sources.getLength(); i++) {
      String filename = sources.get(i).asString().getString();
      newSources.add(serverPrefix + filename);
    }
    json.put("sources", newSources);
  }

  /**
   * Returns a sorted list of all the directories containing at least one filename
   * in the source map.
   */
  List<String> getSourceDirectories() {
    JsonArray sources = (JsonArray) json.get("sources");
    Set<String> directories = new HashSet<String>();
    for (int i = 0; i < sources.getLength(); i++) {
      String filename = sources.get(i).asString().getString();
      directories.add(new File(filename).getParent());
    }

    List<String> result = new ArrayList<String>();
    result.addAll(directories);
    Collections.sort(result);
    return result;
  }

  /**
   * Returns a sorted list of all filenames in the given directory.
   */
  List<String> getSourceFilesInDirectory(String parent) {
    if (!parent.endsWith("/")) {
      throw new IllegalArgumentException("unexpected: " + parent);
    }

    JsonArray sources = (JsonArray) json.get("sources");

    List<String> result = new ArrayList<String>();
    for (int i = 0; i < sources.getLength(); i++) {
      File candidate = new File(sources.get(i).asString().getString());
      if (parent.equals(candidate.getParent() + "/")) {
        result.add(candidate.getName());
      }
    }

    return result;
  }

  String serialize() {
    StringWriter buffer = new StringWriter();
    try {
      json.write(buffer);
    } catch (IOException e) {
      throw new RuntimeException("can't convert sourcemap to json");
    }
    return buffer.toString();
  }
}
