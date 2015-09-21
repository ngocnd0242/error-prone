/*
 * Copyright 2015 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.MATURE;
import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;

import com.google.common.base.Joiner;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.CompilationUnitTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author cushon@google.com (Liam Miller-Cushon)
 */
@BugPattern(
  name = "MultipleTopLevelClasses",
  altNames = {"TopLevel"},
  summary = "Source files should not contain multiple top-level class declarations",
  category = JDK,
  severity = WARNING,
  maturity = MATURE,
  documentSuppression = false
)
public class MultipleTopLevelClasses extends BugChecker implements CompilationUnitTreeMatcher {

  @Override
  public Description matchCompilationUnit(CompilationUnitTree tree, VisitorState state) {
    if (tree.getTypeDecls().size() <= 1) {
      // package-info.java files have zero top-level declarations, everything
      // else should have exactly one.
      return Description.NO_MATCH;
    }
    if (tree.getPackageName() == null) {
      // Real code doesn't use the default package.
      return Description.NO_MATCH;
    }
    List<String> names = new ArrayList<>();
    for (Tree member : tree.getTypeDecls()) {
      if (member instanceof ClassTree) {
        ClassTree classMember = (ClassTree) member;
        switch (classMember.getKind()) {
          case CLASS:
          case INTERFACE:
          case ANNOTATION_TYPE:
          case ENUM:
            SuppressWarnings suppression =
                ASTHelpers.getAnnotation(classMember, SuppressWarnings.class);
            if (suppression != null
                && !Collections.disjoint(Arrays.asList(suppression.value()), allNames())) {
              // If any top-level classes have @SuppressWarnings("TopLevel"), ignore
              // this compilation unit. We can't rely on the normal suppression
              // mechanism because the only enclosing element is the package declaration,
              // and @SuppressWarnings can't be applied to packages.
              return Description.NO_MATCH;
            }
            names.add(classMember.getSimpleName().toString());
            break;
          default:
            break;
        }
      }
    }
    String message =
        String.format(
            "Expected at most one top-level class declaration, instead found: %s",
            Joiner.on(", ").join(names));
    return buildDescription(tree.getPackageName()).setMessage(message).build();
  }
}
