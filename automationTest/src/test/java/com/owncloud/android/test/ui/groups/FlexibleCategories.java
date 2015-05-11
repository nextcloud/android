package com.owncloud.android.test.ui.groups;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.junit.experimental.categories.Categories.CategoryFilter;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.experimental.categories.Category;
import org.junit.runner.Description;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * This class is based on org.junit.experimental.categories.Categories from JUnit 4.10.
 *
 * All anotations and inner classes from the original class Categories are removed,
 * since they will be re-used.
 * Unfortunately sub-classing Categories did not work.
 */
public class FlexibleCategories extends Suite {

 /**
  * Specifies the package which should be scanned for test classes (e.g. @TestScanPackage("my.package")).
  * This annotation is required.
  */
 @Retention(RetentionPolicy.RUNTIME)
 public @interface TestScanPackage {
  public String value();
 }

 /**
  * Specifies the prefix of matching class names (e.g. @TestClassPrefix("Test")).
  * This annotation is optional (default: "").
  */
 @Retention(RetentionPolicy.RUNTIME)
 public @interface TestClassPrefix {
  public String value();
 }

 /**
  * Specifies the suffix of matching class names (e.g. @TestClassSuffix("Test")).
  * This annotation is optional (default: "Test").
  */
 @Retention(RetentionPolicy.RUNTIME)
 public @interface TestClassSuffix {
  public String value();
 }

 /**
  * Specifies an annotation for methods which must be present in a matching class (e.g. @TestMethodAnnotationFilter(Test.class)).
  * This annotation is optional (default: org.junit.Test.class).
  */
 @Retention(RetentionPolicy.RUNTIME)
 public @interface TestMethodAnnotation {
  public Class<? extends Annotation> value();
 }

 public FlexibleCategories(Class<?> clazz, RunnerBuilder builder)
   throws InitializationError {
  this(builder, clazz, PatternClasspathClassesFinder.getSuiteClasses(
    getTestScanPackage(clazz), getTestClassPrefix(clazz), getTestClassSuffix(clazz),
    getTestMethodAnnotation(clazz)));
  try {
   filter(new CategoryFilter(getIncludedCategory(clazz),
     getExcludedCategory(clazz)));
  } catch (NoTestsRemainException e) {
   // Ignore all classes with no matching tests.
  }
  assertNoCategorizedDescendentsOfUncategorizeableParents(getDescription());
 }

 public FlexibleCategories(RunnerBuilder builder, Class<?> clazz,
   Class<?>[] suiteClasses) throws InitializationError {
  super(builder, clazz, suiteClasses);
 }

 private static String getTestScanPackage(Class<?> clazz) throws InitializationError {
  TestScanPackage annotation = clazz.getAnnotation(TestScanPackage.class);
  if (annotation == null) {
   throw new InitializationError("No package given to scan for tests!\nUse the annotation @TestScanPackage(\"my.package\") on the test suite " + clazz + ".");
  }
  return annotation.value();
 }

 private static String getTestClassPrefix(Class<?> clazz) {
  TestClassPrefix annotation = clazz.getAnnotation(TestClassPrefix.class);
  return annotation == null ? "" : annotation.value();
 }

 private static String getTestClassSuffix(Class<?> clazz) {
  TestClassSuffix annotation = clazz.getAnnotation(TestClassSuffix.class);
  return annotation == null ? "Test" : annotation.value();
 }

 private static Class<? extends Annotation> getTestMethodAnnotation(Class<?> clazz) {
  TestMethodAnnotation annotation = clazz.getAnnotation(TestMethodAnnotation.class);
  return annotation == null ? Test.class : annotation.value();
 }

 private Class<?> getIncludedCategory(Class<?> clazz) {
  IncludeCategory annotation= clazz.getAnnotation(IncludeCategory.class);
  return annotation == null ? null : annotation.value();
 }

 private Class<?> getExcludedCategory(Class<?> clazz) {
  ExcludeCategory annotation= clazz.getAnnotation(ExcludeCategory.class);
  return annotation == null ? null : annotation.value();
 }

 private void assertNoCategorizedDescendentsOfUncategorizeableParents(Description description) throws InitializationError {
  if (!canHaveCategorizedChildren(description))
   assertNoDescendantsHaveCategoryAnnotations(description);
  for (Description each : description.getChildren())
   assertNoCategorizedDescendentsOfUncategorizeableParents(each);
 }

 private void assertNoDescendantsHaveCategoryAnnotations(Description description) throws InitializationError {
  for (Description each : description.getChildren()) {
   if (each.getAnnotation(Category.class) != null)
    throw new InitializationError("Category annotations on Parameterized classes are not supported on individual methods.");
   assertNoDescendantsHaveCategoryAnnotations(each);
  }
 }

 // If children have names like [0], our current magical category code can't determine their
 // parentage.
 private static boolean canHaveCategorizedChildren(Description description) {
  for (Description each : description.getChildren())
   if (each.getTestClass() == null)
    return false;
  return true;
 }
}