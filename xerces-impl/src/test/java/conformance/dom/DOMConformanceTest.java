/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package conformance.dom;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Data-driven JUnit 5 test suite executing the W3C DOM Conformance Test Suite (DOM TS)
 * across Level 1 Core, Level 2 Core, Level 3 Core, and Level 3 LS.
 */
public class DOMConformanceTest {

    private static final Set<String> KNOWN_EXCLUSIONS = new HashSet<String>();

    static {
        loadExclusions();
    }

    private static void loadExclusions() {
        try (InputStream is = DOMConformanceTest.class.getResourceAsStream("/conformance/dom/dom-exclusions.txt")) {
            if (is != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            KNOWN_EXCLUSIONS.add(line);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static File findDomTsDir() {
        String sysProp = System.getProperty("domts.dir");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            File f = new File(sysProp);
            if (f.exists()) {
                return f;
            }
        }

        String[] candidates = {
                "build/domts-suite",
                "build/test-tmp/domts",
                "../build/domts-suite",
                "../build/test-tmp/domts",
                "tests/conformance/domts",
                "domts-suite"
        };

        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    @TestFactory
    @DisplayName("W3C DOM Level 1 Core Conformance")
    public Stream<DynamicNode> domLevel1CoreSuite() throws Exception {
        return buildSuite("dom1-core-tests-20040405.jar", "org.w3c.domts.level1.core.alltests", "DOM Level 1 Core");
    }

    @TestFactory
    @DisplayName("W3C DOM Level 2 Core Conformance")
    public Stream<DynamicNode> domLevel2CoreSuite() throws Exception {
        return buildSuite("dom2-core-tests-20040405.jar", "org.w3c.domts.level2.core.alltests", "DOM Level 2 Core");
    }

    @TestFactory
    @DisplayName("W3C DOM Level 3 Core Conformance")
    public Stream<DynamicNode> domLevel3CoreSuite() throws Exception {
        return buildSuite("dom3-core-tests-20040405.jar", "org.w3c.domts.level3.core.alltests", "DOM Level 3 Core");
    }

    @TestFactory
    @DisplayName("W3C DOM Level 3 Load and Save Conformance")
    public Stream<DynamicNode> domLevel3LsSuite() throws Exception {
        return buildSuite("dom3-ls-tests-20040405.jar", "org.w3c.domts.level3.ls.alltests", "DOM Level 3 LS");
    }

    private Stream<DynamicNode> buildSuite(String jarName, String suiteClassName, String title) throws Exception {
        File domtsDir = findDomTsDir();
        Assumptions.assumeTrue(domtsDir != null && domtsDir.exists(),
                "DOM TS directory not found. Please run Gradle download task or set -Ddomts.dir");

        File jarFile = new File(domtsDir, jarName);
        Assumptions.assumeTrue(jarFile.exists(), "DOM TS JAR not found: " + jarFile.getAbsolutePath());

        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarFile.toURI().toURL()}, getClass().getClassLoader());

        Class<?> suiteClass = Class.forName(suiteClassName, true, classLoader);
        Class<?> factoryClass = Class.forName("org.w3c.domts.DOMTestDocumentBuilderFactory", true, classLoader);
        Class<?> jaxpFactoryClass = Class.forName("org.w3c.domts.JAXPDOMTestDocumentBuilderFactory", true, classLoader);
        Class<?> sinkClass = Class.forName("org.w3c.domts.DOMTestSink", true, classLoader);

        Method getConfig1 = jaxpFactoryClass.getMethod("getConfiguration1");
        Object config1 = getConfig1.invoke(null);

        Constructor<?> jaxpConstructor = jaxpFactoryClass.getConstructor(
                DocumentBuilderFactory.class,
                Class.forName("[Lorg.w3c.domts.DocumentBuilderSetting;", true, classLoader)
        );
        Object builderFactory = jaxpConstructor.newInstance(DocumentBuilderFactory.newInstance(), config1);

        Constructor<?> suiteConstructor = suiteClass.getConstructor(factoryClass);
        Object domTestSuite = suiteConstructor.newInstance(builderFactory);

        List<Class<?>> testClasses = new ArrayList<Class<?>>();

        Object sinkProxy = Proxy.newProxyInstance(
                classLoader,
                new Class<?>[]{sinkClass},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("addTest".equals(method.getName()) && args != null && args.length == 1) {
                            testClasses.add((Class<?>) args[0]);
                            return null;
                        }
                        return null;
                    }
                }
        );

        Method buildMethod = suiteClass.getMethod("build", sinkClass);
        buildMethod.invoke(domTestSuite, sinkProxy);

        List<DynamicNode> tests = new ArrayList<DynamicNode>();
        for (Class<?> testClass : testClasses) {
            String testName = testClass.getSimpleName();
            tests.add(DynamicTest.dynamicTest(testName, () -> {
                runDomTest(testClass, builderFactory);
            }));
        }

        return Stream.of(DynamicContainer.dynamicContainer(title + " (" + tests.size() + " tests)", tests));
    }

    private void runDomTest(Class<?> testClass, Object builderFactory) throws Throwable {
        String testName = testClass.getSimpleName();
        if (KNOWN_EXCLUSIONS.contains(testName)) {
            Assumptions.assumeTrue(false, "Skipping known excluded test: " + testName);
        }

        ClassLoader originalCl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(testClass.getClassLoader());
        try {
            Class<?> factoryParamClass = Class.forName("org.w3c.domts.DOMTestDocumentBuilderFactory", true, testClass.getClassLoader());
            Constructor<?> constructor;
            Object testInstance;
            try {
                constructor = testClass.getConstructor(factoryParamClass);
                testInstance = constructor.newInstance(builderFactory);
            } catch (NoSuchMethodException e) {
                constructor = testClass.getConstructor();
                testInstance = constructor.newInstance();
            }

            Class<?> domTestCaseClass = Class.forName("org.w3c.domts.DOMTestCase", true, testClass.getClassLoader());
            Class<?> adapterClass = Class.forName("org.w3c.domts.JUnitTestCaseAdapter", true, testClass.getClassLoader());
            Constructor<?> adapterConstructor = adapterClass.getConstructor(domTestCaseClass);
            Object adapter = adapterConstructor.newInstance(testInstance);

            Method setFrameworkMethod = domTestCaseClass.getMethod("setFramework", Class.forName("org.w3c.domts.DOMTestFramework", true, testClass.getClassLoader()));
            setFrameworkMethod.invoke(testInstance, adapter);

            Method runTestMethod = testClass.getMethod("runTest");
            runTestMethod.invoke(testInstance);
        } catch (InvocationTargetException ite) {
            Throwable target = ite.getTargetException();
            String exClassName = target.getClass().getName();
            if ("org.w3c.domts.DOMTestIncompatibleException".equals(exClassName)) {
                Assumptions.assumeTrue(false, "Test skipped due to incompatible parser configuration: " + target.getMessage());
            }
            throw target;
        } finally {
            Thread.currentThread().setContextClassLoader(originalCl);
        }
    }
}
