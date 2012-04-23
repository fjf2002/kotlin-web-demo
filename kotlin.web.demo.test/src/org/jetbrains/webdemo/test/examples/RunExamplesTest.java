/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.webdemo.test.examples;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.webdemo.Initializer;
import org.jetbrains.webdemo.responseHelpers.CompileAndRunExecutor;
import org.jetbrains.webdemo.responseHelpers.JsConverter;
import org.jetbrains.webdemo.server.ApplicationSettings;
import org.jetbrains.webdemo.session.SessionInfo;
import org.jetbrains.webdemo.test.BaseTest;
import org.jetbrains.webdemo.test.TestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Natalia.Ukhorskaya
 */


public class RunExamplesTest extends BaseTest {

    private static ArrayList<String> jsExamples = new ArrayList<String>();
    private static Map<String, Example> expectedResults = new HashMap<String, Example>();

    public static Test suite() {
        expectedResults.put("Null-checks.kt", new Example("Null-checks.kt", "2 3", "6<br/>"));
        expectedResults.put("Use a conditional expression.kt", new Example("Use a conditional expression.kt", "10 20", "20<br/>"));
        expectedResults.put("is-checks and smart casts.kt", new Example("is-checks and smart casts.kt", "", "3<br/>null<br/>"));
        expectedResults.put("Use a while-loop.kt", new Example("Use a while-loop.kt", "guest1 guest2 guest3 guest4", "guest1<br/>guest2<br/>guest3<br/>guest4<br/>"));
        expectedResults.put("Use a for-loop.kt", new Example("Use a for-loop.kt", "guest1 guest2 guest3", "guest1<br/>guest2<br/>guest3<br/><br/>guest1<br/>guest2<br/>guest3<br/>"));
        expectedResults.put("Use ranges and in.kt", new Example("Use ranges and in.kt", "4", "OK<br/>1 2 3 4 5 <br/>Out: array has only 3 elements. x = 4<br/>Yes: array contains aaa<br/>No: array doesn't contains ddd<br/>"));
        expectedResults.put("Use when.kt", new Example("Use when.kt", "", "Greeting<br/>One<br/>Long<br/>Not a string<br/>Unknown<br/>"));
        expectedResults.put("Creatures.kt", new Example("Creatures.kt", "", "from js file"));
        expectedResults.put("Fancy lines.kt", new Example("Fancy lines.kt", "", "from js file"));
        expectedResults.put("Hello, Kotlin.kt", new Example("Hello, Kotlin.kt", "", "from js file"));
        expectedResults.put("Traffic light.kt", new Example("Traffic light.kt", "", "from js file"));
        expectedResults.put("A multi-language Hello.kt", new Example("A multi-language Hello.kt", "FR", "Salut!<br/>"));
        expectedResults.put("An object-oriented Hello.kt", new Example("An object-oriented Hello.kt", "guest1", "Hello, guest1<br/>"));
        expectedResults.put("Reading a name from the command line.kt", new Example("Reading a name from the command line.kt", "guest1", "Hello, guest1!<br/>"));
        expectedResults.put("Reading many names from the command line.kt", new Example("Reading many names from the command line.kt", "guest1 guest2 guest3", "Hello, guest1!<br/>Hello, guest2!<br/>Hello, guest3!<br/>"));
        expectedResults.put("Simplest version.kt", new Example("Simplest version.kt", "", "Hello, world!<br/>"));
        expectedResults.put("99 Bottles of Beer.kt", new Example("99 Bottles of Beer.kt", "", "from txt file"));
        expectedResults.put("HTML Builder.kt", new Example("HTML Builder.kt", "", "from txt file"));
        expectedResults.put("Maze.kt", new Example("Maze.kt", "", "from txt file"));
        expectedResults.put("Life.kt", new Example("Life.kt", "", "from txt file"));
        expectedResults.put("", new Example("", "", ""));

        jsExamples.add("is-checks and smart casts.kt");
        jsExamples.add("Use a while-loop.kt");
        jsExamples.add("Use a for-loop.kt");
        jsExamples.add("Simplest version.kt");
        jsExamples.add("Reading a name from the command line.kt");
        jsExamples.add("Reading many names from the command line.kt");

        jsExamples.add("A multi-language Hello.kt");
        jsExamples.add("An object-oriented Hello.kt");
        //jsExamples.add("HTML Builder.kt");


        TestSuite suite = new TestSuite();
        TestSuite ats = new TestSuite();
        suite.addTest(ats);
        File parsingSourceDir = new File(ApplicationSettings.EXAMPLES_DIRECTORY);
        addFilesFromDirToSuite(parsingSourceDir, ats);
        return suite;
    }

    private static void addFilesFromDirToSuite(File file, TestSuite ats) {
        if (file.isDirectory()) {
            for (File sourceFile : file.listFiles()) {
                if (!file.getName().equals("Problems")) {
                    addFilesFromDirToSuite(sourceFile, ats);
                }
            }
        }
        else {
            if (file.getName().equals("order.txt")) {
                return;
            }
            if (file.getName().endsWith(".kt")) {
                if (file.getParentFile().getName().equals("Canvas")) {
                    //TODO add execution for js (without order of function in generated js
                    //ats.addTest(new RunExamplesTest(file, "canvas"));
                }
                else {
                    /*if (jsExamples.contains(file.getName())) {
                        ats.addTest(new RunExamplesTest(file, "js"));
                    }*/
                    ats.addTest(new RunExamplesTest(file, "java"));
                }
            }
        }
    }


    private final File sourceFile;
    private final String runConf;

    public RunExamplesTest(File sourceFile, String runConf) {
        super(sourceFile.getName());
        this.sourceFile = sourceFile;
        this.runConf = runConf;
    }

    @Override
    protected void runTest() throws Throwable {
        compareResponseAndExpectedResult(sourceFile, runConf);
    }

    private void compareResponseAndExpectedResult(File file, String runConfiguration) throws IOException, JSONException {
        sessionInfo.setType(SessionInfo.TypeOfRequest.RUN);
        sessionInfo.setRunConfiguration(runConfiguration);

        Example example = getExampleInfo(file.getName());

        assertEquals("Cannot find info for example " + file.getName(), true, example != null);

        StringBuilder output = new StringBuilder();
        String actualResult;
        if (sessionInfo.getRunConfiguration().equals(SessionInfo.RunConfiguration.JAVA)) {
            JetFile currentPsiFile = JetPsiFactory.createFile(Initializer.INITIALIZER.getEnvironment().getProject(), TestUtils.getDataFromFile(file));
            sessionInfo.setType(SessionInfo.TypeOfRequest.RUN);

            assert example != null;
            CompileAndRunExecutor responseForCompilation = new CompileAndRunExecutor(currentPsiFile, example.getArgs(), sessionInfo);
            actualResult = responseForCompilation.getResult();

            JSONArray actualArray = new JSONArray(actualResult);


            for (int i = 0; i < actualArray.length(); i++) {
                JSONObject object = (JSONObject) actualArray.get(i);
                if (object.get("type").equals("out")) {
                    output.append(object.get("text"));
                }
            }
        } else {
            sessionInfo.setType(SessionInfo.TypeOfRequest.CONVERT_TO_JS);
            assert example != null;
            actualResult = new JsConverter(sessionInfo).getResult(TestUtils.getDataFromFile(file), example.getArgs());
            JSONArray actualArray = new JSONArray(actualResult);

            JSONObject object = (JSONObject) actualArray.get(0);
            output.append(object.get("text"));
        }


        assertEquals("Wrong result for " + file.getName(), example.getResult(), output.toString());
    }

    private Example getExampleInfo(String name) {
        return expectedResults.get(name);
    }
}

