/*******************************************************************************
 * Copyright (c) 2009, 2022 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.cli.internal.commands;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;

import org.jacoco.cli.internal.CommandTestBase;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataWriter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for {@link Report}.
 */
public class ReportTest extends CommandTestBase {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void should_print_usage_when_no_options_are_given()
            throws Exception {
        execute("report");

        assertFailure();
        assertContains("\"--classfiles\"", err);
        assertContains(
                "Usage: java -jar jacococli.jar report [<execfiles> ...]", err);
    }

    @Test
    public void should_print_warning_when_no_exec_files_are_provided()
            throws Exception {
        execute("report", "--classfiles", getClassPath());

        assertOk();
        assertContains("[WARN] No execution data files provided.", out);
    }

    @Test
    public void should_print_number_of_analyzed_classes() throws Exception {
        execute("report", "--classfiles", getClassPath());

        assertOk();
        assertContains("[INFO] Analyzing 14 classes.", out);
    }

    @Test
    public void should_print_warning_when_exec_data_does_not_match()
            throws Exception {
        File exec = new File(tmp.getRoot(), "jacoco.exec");
        final FileOutputStream execout = new FileOutputStream(exec);
        ExecutionDataWriter writer = new ExecutionDataWriter(execout);
        // Add probably invalid id for this test class:
        writer.visitClassExecution(
                new ExecutionData(0x123, getClass().getName().replace('.', '/'),
                        new boolean[]{true}));
        execout.close();

        execute("report", exec.getAbsolutePath(), "--classfiles",
                getClassPath());

        assertOk();
        assertContains("[WARN] Some classes do not match with execution data.",
                out);
        assertContains(
                "[WARN] For report generation the same class files must be used as at runtime.",
                out);
        assertContains(
                "[WARN] Execution data for class org/jacoco/cli/internal/commands/ReportTest does not match.",
                out);
    }

    @Test
    public void should_create_xml_report_when_xml_option_is_provided()
            throws Exception {
        File xml = new File(tmp.getRoot(), "coverage.xml");

        execute("report", "--classfiles", getClassPath(), "--xml",
                xml.getAbsolutePath());

        assertOk();
        assertTrue(xml.isFile());
    }

    @Test
    public void should_create_csv_report_when_csv_option_is_provided()
            throws Exception {
        File csv = new File(tmp.getRoot(), "coverage.csv");

        execute("report", "--classfiles", getClassPath(), "--csv",
                csv.getAbsolutePath());

        assertOk();
        assertTrue(csv.isFile());
    }

    @Test
    public void should_create_html_report_when_html_option_is_provided()
            throws Exception {
        File html = new File(tmp.getRoot(), "coverage");

        execute("report", "--classfiles", getClassPath(), "--sourcefiles",
                "./src", "--html", html.getAbsolutePath());

        assertOk();
        assertTrue(html.isDirectory());
        assertTrue(new File(html,
                "org.jacoco.cli.internal.commands/ReportTest.html").isFile());
        assertTrue(new File(html,
                "org.jacoco.cli.internal.commands/ReportTest.java.html")
                .isFile());
    }

    @Test
    public void mytest() throws Exception {

        execute("report", "D:\\jacoco\\jacoco-demo.exec", "--classfiles",
                "D:\\IdeaProjects\\base\\base-service\\application\\target\\classes\\com",
                "--sourcefiles",
                "D:\\IdeaProjects\\base\\base-service\\application\\src\\main\\java ",
                "--html", "D:\\jacoco\\report", "--xml",
                "D:\\jacoco\\report.xml", "--diffCode",
                " [\n" + "    {\n"
                        + "      \"classFile\": \"com/dr/application/app/controller/LoginController\",\n"
                        + "      \"methodInfos\": [\n" + "        {\n"
                        + "          \"methodName\": \"testInt\",\n"
                        + "          \"parameters\": \"Map<String,Object>&List<String>&Set<Integer>\"\n"
                        + "        },\n" + "        {\n"
                        + "          \"methodName\": \"display\",\n"
                        + "          \"parameters\": \"\"\n" + "        },\n"
                        + "        {\n" + "          \"methodName\": \"a\",\n"
                        + "          \"parameters\": \"InnerClass\"\n"
                        + "        }\n" + "      ],\n"
                        + "      \"type\": \"MODIFY\"\n" + "    }\n" + "  ]");
        // execute("report","D:\\jacoco\\jacoco-demo.exec", "--classfiles",
        // "D:\\IdeaProjects\\base\\base-service\\application\\target\\classes\\com",
        // "--sourcefiles",
        // "D:\\IdeaProjects\\base\\base-service\\application\\src\\main\\java
        // ", "--html", "D:\\jacoco\\all\\report","--xml",
        // "D:\\jacoco\\report.xml");

        assertOk();
        // assertTrue(html.isDirectory());
        // assertTrue(new File(html,
        // "org.jacoco.cli.internal.commands/ReportTest.html").isFile());
        // assertTrue(new File(html,
        // "org.jacoco.cli.internal.commands/ReportTest.java.html")
        // .isFile());
    }

    @Test
    public void mytest2() throws Exception {

        execute("report", "/Users/rayduan/jacoco/jacoco-demo.exec",
                "--classfiles",
                "/Users/rayduan/IdeaProjects\\base\\base-service\\application\\target\\classes\\com",
                "--sourcefiles",
                "/Users/rayduan/IdeaProjects\\base\\base-service\\application\\src\\main\\java ",
                "--html", "/Users/rayduan/jacoco/report", "--xml",
                "/Users/rayduan/jacoco/report.xml", "--diffCodeFiles", "");
        assertOk();
    }

    @Test
    public void should_use_all_values_when_multiple_classfiles_options_are_provided()
            throws Exception {
        File html = new File(tmp.getRoot(), "coverage");

        final String c1 = getClassPath()
                + "/org/jacoco/cli/internal/commands/ReportTest.class";
        final String c2 = getClassPath()
                + "/org/jacoco/cli/internal/commands/DumpTest.class";

        execute("report", "--classfiles", c1, "--classfiles", c2, "--html",
                html.getAbsolutePath());

        assertOk();
        assertTrue(html.isDirectory());
        assertTrue(new File(html,
                "org.jacoco.cli.internal.commands/ReportTest.html").isFile());
        assertTrue(
                new File(html, "org.jacoco.cli.internal.commands/DumpTest.html")
                        .isFile());
    }

    @Test
    public void mytest3() throws Exception {
        execute("report",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/coverage_3.exec",
                "--middleexec",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/coverage_2.exec",
                "--middleexec",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/coverage_1.exec",
                "--middleclassfiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/class2",
                "--middleclassfiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/class1",
                "--classfiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/class3",
                "--sourcefiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data/java_source/java",
                "--html", "/Users/cc/Project/MyProject/code-diff/html1229",
                "--diffCode", "[{\"classFile\":\"com/htsc/android/mcrm/test/HtscTestActivity\",\"lines\":[{\"endLineNum\":218,\"startLineNum\":217,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"onClick\",\"parameters\":[\"View\"]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"},{\"classFile\":\"com/htsc/android/mcrm/main/MainActivity\",\"lines\":[{\"endLineNum\":1317,\"startLineNum\":1316,\"type\":\"INSERT\"},{\"endLineNum\":1326,\"startLineNum\":1321,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"test1\",\"parameters\":[]},{\"methodName\":\"test2\",\"parameters\":[]},{\"methodName\":\"test3\",\"parameters\":[]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"}]",
                "--middleDiffCode", "[{\"classFile\":\"com/htsc/android/mcrm/test/HtscTestActivity\",\"lines\":[{\"endLineNum\":218,\"startLineNum\":217,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"onClick\",\"parameters\":[\"View\"]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"},{\"classFile\":\"com/htsc/android/mcrm/main/MainActivity\",\"lines\":[{\"endLineNum\":1328,\"startLineNum\":1324,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"test3\",\"parameters\":[]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"}]",
                "--middleDiffCode", "[{\"classFile\":\"com/htsc/android/mcrm/test/HtscTestActivity\",\"lines\":[{\"endLineNum\":218,\"startLineNum\":217,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"onClick\",\"parameters\":[\"View\"]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"},{\"classFile\":\"com/htsc/android/mcrm/main/MainActivity\",\"lines\":[{\"endLineNum\":1326,\"startLineNum\":1321,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"test2\",\"parameters\":[]},{\"methodName\":\"test3\",\"parameters\":[]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"}]"
        );
        assertOk();
    }

    @Test
    public void mytest4() throws Exception {
        execute("report",
                "/Users/cc/Project/MyProject/code-diff-source/test_data_2/final.exec",
                "--middleexec",
                "/Users/cc/Project/MyProject/code-diff-source/test_data_2/middle.exec",
                "--middleclassfiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data_2/class_middle",
                "--classfiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data_2/class_final",
                "--sourcefiles",
                "/Users/cc/Project/MyProject/code-diff-source/test_data_2/source/java",
                "--html", "/Users/cc/Project/MyProject/code-diff/html0103",
                "--xml", "/Users/cc/Project/MyProject/code-diff/xml0103.xml",
                "--diffCode", "[{\"classFile\":\"com/lphtsccft/zhangle/test/HtscTestActivity\",\"lines\":[{\"endLineNum\":9,\"startLineNum\":8,\"type\":\"INSERT\"},{\"endLineNum\":203,\"startLineNum\":202,\"type\":\"INSERT\"},{\"endLineNum\":219,\"startLineNum\":218,\"type\":\"INSERT\"},{\"endLineNum\":223,\"startLineNum\":222,\"type\":\"INSERT\"},{\"endLineNum\":246,\"startLineNum\":245,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"onClick\",\"parameters\":[\"View\"]},{\"methodName\":\"updateDb\",\"parameters\":[]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"}, {\"classFile\":\"com/lphtsccft/zhangle/otherfunction/share/HtscShareActivity\",\"lines\":[{\"endLineNum\":78,\"startLineNum\":77,\"type\":\"INSERT\"},{\"endLineNum\":458,\"startLineNum\":457,\"type\":\"INSERT\"},{\"endLineNum\":530,\"startLineNum\":529,\"type\":\"INSERT\"},{\"endLineNum\":537,\"startLineNum\":536,\"type\":\"INSERT\"},{\"endLineNum\":541,\"startLineNum\":540,\"type\":\"INSERT\"},{\"endLineNum\":575,\"startLineNum\":574,\"type\":\"INSERT\"},{\"endLineNum\":765,\"startLineNum\":764,\"type\":\"INSERT\"},{\"endLineNum\":809,\"startLineNum\":807,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"onCreate\",\"parameters\":[\"Bundle\"]},{\"methodName\":\"handleMessage\",\"parameters\":[\"Message\"]},{\"methodName\":\"shareQiYeWeiLink\",\"parameters\":[]},{\"methodName\":\"shareWeiXinMiniProgram\",\"parameters\":[]},{\"methodName\":\"shareWeixinImage\",\"parameters\":[\"int\"]}],\"moduleName\":\"src\",\"type\":\"MODIFY\"}]",
                "--middleDiffCode", "[{\"classFile\":\"com/lphtsccft/zhangle/test/HtscTestActivity\",\"lines\":[{\"endLineNum\":203,\"startLineNum\":202,\"type\":\"INSERT\"},{\"endLineNum\":246,\"startLineNum\":245,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"onClick\",\"parameters\":[\"View\"]},{\"methodName\":\"updateDb\",\"parameters\":[]}],\"moduleName\":\"app\",\"type\":\"MODIFY\"}, {\"classFile\":\"com/lphtsccft/zhangle/otherfunction/share/HtscShareActivity\",\"lines\":[{\"endLineNum\":575,\"startLineNum\":574,\"type\":\"INSERT\"},{\"endLineNum\":765,\"startLineNum\":764,\"type\":\"INSERT\"},{\"endLineNum\":809,\"startLineNum\":808,\"type\":\"INSERT\"}],\"methodInfos\":[{\"methodName\":\"shareQiYeWeiLink\",\"parameters\":[]},{\"methodName\":\"shareWeiXinMiniProgram\",\"parameters\":[]},{\"methodName\":\"shareWeixinImage\",\"parameters\":[\"int\"]}],\"moduleName\":\"src\",\"type\":\"MODIFY\"}]"
        );
        assertOk();
    }

}
