/*
 * Copyright The OpenTelemetry Authors
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

package io.opentelemetry.auto.tooling.muzzle;

import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.BOOTSTRAP_LOADER;

import io.opentelemetry.auto.bootstrap.WeakCache;
import io.opentelemetry.auto.tooling.AgentTooling;
import io.opentelemetry.auto.tooling.Utils;
import io.opentelemetry.auto.tooling.muzzle.Reference.Mismatch;
import io.opentelemetry.auto.tooling.muzzle.Reference.Source;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.description.field.FieldDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.pool.TypePool;

/** Matches a set of references against a classloader. */
@Slf4j
public final class ReferenceMatcher {
  private final WeakCache<ClassLoader, Boolean> mismatchCache = AgentTooling.newWeakCache();
  private final Reference[] references;
  private final Set<String> helperClassNames;

  public ReferenceMatcher(final Reference... references) {
    this(new String[0], references);
  }

  public ReferenceMatcher(final String[] helperClassNames, final Reference[] references) {
    this.references = references;
    this.helperClassNames = new HashSet<>(Arrays.asList(helperClassNames));
  }

  public Reference[] getReferences() {
    return references;
  }

  /**
   * Matcher used by ByteBuddy. Fails fast and only caches empty results, or complete results
   *
   * @param loader Classloader to validate against (or null for bootstrap)
   * @return true if all references match the classpath of loader
   */
  public boolean matches(ClassLoader loader) {
    if (loader == BOOTSTRAP_LOADER) {
      loader = Utils.getBootstrapProxy();
    }
    final ClassLoader cl = loader;
    return mismatchCache.getIfPresentOrCompute(
        loader,
        new Callable<Boolean>() {
          @Override
          public Boolean call() {
            return doesMatch(cl);
          }
        });
  }

  private boolean doesMatch(final ClassLoader loader) {
    for (final Reference reference : references) {
      // Don't reference-check helper classes.
      // They will be injected by the instrumentation's HelperInjector.
      if (!helperClassNames.contains(reference.getClassName())) {
        if (!checkMatch(reference, loader).isEmpty()) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Loads the full list of mismatches. Used in debug contexts only
   *
   * @param loader Classloader to validate against (or null for bootstrap)
   * @return A list of all mismatches between this ReferenceMatcher and loader's classpath.
   */
  public List<Reference.Mismatch> getMismatchedReferenceSources(ClassLoader loader) {
    if (loader == BOOTSTRAP_LOADER) {
      loader = Utils.getBootstrapProxy();
    }

    List<Mismatch> mismatches = Collections.emptyList();

    for (final Reference reference : references) {
      // Don't reference-check helper classes.
      // They will be injected by the instrumentation's HelperInjector.
      if (!helperClassNames.contains(reference.getClassName())) {
        mismatches = lazyAddAll(mismatches, checkMatch(reference, loader));
      }
    }

    return mismatches;
  }

  /**
   * Check a reference against a classloader's classpath.
   *
   * @param loader
   * @return A list of mismatched sources. A list of size 0 means the reference matches the class.
   */
  private static List<Reference.Mismatch> checkMatch(
      final Reference reference, final ClassLoader loader) {
    final TypePool typePool =
        AgentTooling.poolStrategy()
            .typePool(AgentTooling.locationStrategy().classFileLocator(loader), loader);
    try {
      final TypePool.Resolution resolution = typePool.describe(reference.getClassName());
      if (!resolution.isResolved()) {
        return Collections.<Mismatch>singletonList(
            new Mismatch.MissingClass(
                reference.getSources().toArray(new Source[0]), reference.getClassName()));
      }
      return checkMatch(reference, resolution.resolve());
    } catch (final Exception e) {
      if (e.getMessage().startsWith("Cannot resolve type description for ")) {
        // bytebuddy throws an illegal state exception with this message if it cannot resolve types
        // TODO: handle missing type resolutions without catching bytebuddy's exceptions
        final String className = e.getMessage().replace("Cannot resolve type description for ", "");
        return Collections.<Mismatch>singletonList(
            new Mismatch.MissingClass(reference.getSources().toArray(new Source[0]), className));
      } else {
        // Shouldn't happen. Fail the reference check and add a mismatch for debug logging.
        return Collections.<Mismatch>singletonList(
            new Mismatch.ReferenceCheckError(e, reference, loader));
      }
    }
  }

  public static List<Reference.Mismatch> checkMatch(
      final Reference reference, final TypeDescription typeOnClasspath) {
    List<Mismatch> mismatches = Collections.emptyList();

    for (final Reference.Flag flag : reference.getFlags()) {
      if (!flag.matches(typeOnClasspath.getModifiers())) {
        final String desc = reference.getClassName();
        mismatches =
            lazyAdd(
                mismatches,
                new Mismatch.MissingFlag(
                    reference.getSources().toArray(new Source[0]),
                    desc,
                    flag,
                    typeOnClasspath.getModifiers()));
      }
    }

    for (final Reference.Field fieldRef : reference.getFields()) {
      final FieldDescription.InDefinedShape fieldDescription = findField(fieldRef, typeOnClasspath);
      if (fieldDescription == null) {
        mismatches =
            lazyAdd(
                mismatches,
                new Reference.Mismatch.MissingField(
                    fieldRef.getSources().toArray(new Reference.Source[0]),
                    reference.getClassName(),
                    fieldRef.getName(),
                    fieldRef.getType().getInternalName()));
      } else {
        for (final Reference.Flag flag : fieldRef.getFlags()) {
          if (!flag.matches(fieldDescription.getModifiers())) {
            final String desc =
                reference.getClassName()
                    + "#"
                    + fieldRef.getName()
                    + fieldRef.getType().getInternalName();
            mismatches =
                lazyAdd(
                    mismatches,
                    new Mismatch.MissingFlag(
                        fieldRef.getSources().toArray(new Source[0]),
                        desc,
                        flag,
                        fieldDescription.getModifiers()));
          }
        }
      }
    }

    for (final Reference.Method methodRef : reference.getMethods()) {
      final MethodDescription.InDefinedShape methodDescription =
          findMethod(methodRef, typeOnClasspath);
      if (methodDescription == null) {
        mismatches =
            lazyAdd(
                mismatches,
                new Reference.Mismatch.MissingMethod(
                    methodRef.getSources().toArray(new Reference.Source[0]),
                    methodRef.getName(),
                    methodRef.getDescriptor()));
      } else {
        for (final Reference.Flag flag : methodRef.getFlags()) {
          if (!flag.matches(methodDescription.getModifiers())) {
            final String desc =
                reference.getClassName() + "#" + methodRef.getName() + methodRef.getDescriptor();
            mismatches =
                lazyAdd(
                    mismatches,
                    new Mismatch.MissingFlag(
                        methodRef.getSources().toArray(new Source[0]),
                        desc,
                        flag,
                        methodDescription.getModifiers()));
          }
        }
      }
    }
    return mismatches;
  }

  private static boolean matchesPrimitive(String longName, String shortName) {
    // The two meta type systems in use here differ in their treatment of primitive type names....
    return shortName.equals("I") && longName.equals(int.class.getName())
        || shortName.equals("C") && longName.equals(char.class.getName())
        || shortName.equals("Z") && longName.equals(boolean.class.getName())
        || shortName.equals("J") && longName.equals(long.class.getName())
        || shortName.equals("S") && longName.equals(short.class.getName())
        || shortName.equals("F") && longName.equals(float.class.getName())
        || shortName.equals("D") && longName.equals(double.class.getName())
        || shortName.equals("B") && longName.equals(byte.class.getName());
  }

  private static FieldDescription.InDefinedShape findField(
      final Reference.Field fieldRef, final TypeDescription typeOnClasspath) {
    for (final FieldDescription.InDefinedShape fieldType : typeOnClasspath.getDeclaredFields()) {
      if (fieldType.getName().equals(fieldRef.getName())
          && ((fieldType
                  .getType()
                  .asErasure()
                  .getInternalName()
                  .equals(fieldRef.getType().getInternalName()))
              || (fieldType.getType().asErasure().isPrimitive()
                  && matchesPrimitive(
                      fieldType.getType().asErasure().getInternalName(),
                      fieldRef.getType().getInternalName())))) {
        return fieldType;
      }
    }
    if (typeOnClasspath.getSuperClass() != null) {
      final FieldDescription.InDefinedShape fieldOnSupertype =
          findField(fieldRef, typeOnClasspath.getSuperClass().asErasure());
      if (fieldOnSupertype != null) {
        return fieldOnSupertype;
      }
    }
    for (final TypeDescription.Generic interfaceType : typeOnClasspath.getInterfaces()) {
      final FieldDescription.InDefinedShape fieldOnSupertype =
          findField(fieldRef, interfaceType.asErasure());
      if (fieldOnSupertype != null) {
        return fieldOnSupertype;
      }
    }
    return null;
  }

  private static MethodDescription.InDefinedShape findMethod(
      final Reference.Method methodRef, final TypeDescription typeOnClasspath) {
    for (final MethodDescription.InDefinedShape methodDescription :
        typeOnClasspath.getDeclaredMethods()) {
      if (methodDescription.getInternalName().equals(methodRef.getName())
          && methodDescription.getDescriptor().equals(methodRef.getDescriptor())) {
        return methodDescription;
      }
    }
    if (typeOnClasspath.getSuperClass() != null) {
      final MethodDescription.InDefinedShape methodOnSupertype =
          findMethod(methodRef, typeOnClasspath.getSuperClass().asErasure());
      if (methodOnSupertype != null) {
        return methodOnSupertype;
      }
    }
    for (final TypeDescription.Generic interfaceType : typeOnClasspath.getInterfaces()) {
      final MethodDescription.InDefinedShape methodOnSupertype =
          findMethod(methodRef, interfaceType.asErasure());
      if (methodOnSupertype != null) {
        return methodOnSupertype;
      }
    }
    return null;
  }

  // optimization to avoid ArrayList allocation in the common case when there are no mismatches
  private static List<Mismatch> lazyAdd(List<Mismatch> mismatches, Mismatch mismatch) {
    List<Mismatch> result = mismatches.isEmpty() ? new ArrayList<Mismatch>() : mismatches;
    result.add(mismatch);
    return result;
  }

  // optimization to avoid ArrayList allocation in the common case when there are no mismatches
  private static List<Mismatch> lazyAddAll(List<Mismatch> mismatches, List<Mismatch> toAdd) {
    if (!toAdd.isEmpty()) {
      List<Mismatch> result = mismatches.isEmpty() ? new ArrayList<Mismatch>() : mismatches;
      result.addAll(toAdd);
      return result;
    }
    return mismatches;
  }
}
