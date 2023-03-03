/*******************************************************************************
 * Copyright (c) 2009, 2022 Mountainminds GmbH & Co. KG and Contributors
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    John Keeping - initial implementation
 *    Marc R. Hoffmann - rework
 *
 *******************************************************************************/
package org.jacoco.cli.internal.commands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jacoco.cli.internal.Command;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.ExecutionData;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.internal.analysis.ClassAnalyzer;
import org.jacoco.core.internal.analysis.ClassCoverageImpl;
import org.jacoco.core.internal.analysis.MethodCoverageImpl;
import org.jacoco.core.internal.diff.ClassInfoDto;
import org.jacoco.core.internal.diff.CodeDiffUtil;
import org.jacoco.core.internal.diff.JsonReadUtil;
import org.jacoco.core.internal.diff.MethodInfoDto;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;

/**
 * The <code>report</code> command.
 */
public class Report extends Command {

    @Argument(usage = "list of JaCoCo *.exec files to read", metaVar = "<execfiles>")
    List<File> execfiles = new ArrayList<File>();

    @Option(name = "--middleexec", usage = "list of JaCoCo *.exec files to read", metaVar = "<execfiles>")
    List<File> middleExecfiles = new ArrayList<File>();

    @Option(name = "--classfiles", usage = "location of Java class files", metaVar = "<path>", required = true)
    List<File> classfiles = new ArrayList<File>();

    @Option(name = "--middleclassfiles", usage = "location of Java class files", metaVar = "<path>", required = true)
    List<File> middleClassfiles = new ArrayList<File>();

    @Option(name = "--sourcefiles", usage = "location of the source files", metaVar = "<path>")
    List<File> sourcefiles = new ArrayList<File>();

    @Option(name = "--diffCode", usage = "input String for diff", metaVar = "<file>")
    String diffCode;

    @Option(name = "--middleDiffCode", usage = "input String for diff", metaVar = "<file>")
    List<String> oldDiffCode = new ArrayList<>();

    @Option(name = "--diffCodeFiles", usage = "input file for diff", metaVar = "<path>")
    String diffCodeFiles;

    @Option(name = "--tabwith", usage = "tab stop width for the source pages (default 4)", metaVar = "<n>")
    int tabwidth = 4;

    @Option(name = "--name", usage = "name used for this report", metaVar = "<name>")
    String name = "JaCoCo Coverage Report";

    @Option(name = "--encoding", usage = "source file encoding (by default platform encoding is used)", metaVar = "<charset>")
    String encoding;

    @Option(name = "--xml", usage = "output file for the XML report", metaVar = "<file>")
    File xml;

    @Option(name = "--csv", usage = "output file for the CSV report", metaVar = "<file>")
    File csv;

    @Option(name = "--html", usage = "output directory for the HTML report", metaVar = "<dir>")
    File html;

    @Override
    public String description() {
        return "Generate reports in different formats by reading exec and Java class files.";
    }

    @Override
    public int execute(final PrintWriter out, final PrintWriter err)
            throws IOException {
        final ExecFileLoader oldLoader = loadExecutionData(out, middleExecfiles);// 读取exec

        final ExecFileLoader loader = loadExecutionData(out, execfiles);// 读取exec
        // 可以在这个阶段手动修改loader的值来合并覆盖率数据。
        // 在 MethodCoverageImpl 中添加开始行，结束行，probes数组的对象
        // 在 ClassAnalyzer 中会统计 MethodCoverageImpl ClassCoverageImpl ，在这个环节添加数据


        final IBundleCoverage bundle = analyze(loader.getExecutionDataStore(),
                out, classfiles);// 读取class文件

        for (int i = 0; i < getOldDiffData().size(); i++) {
            List<File> tempFiles = new ArrayList<File>();
            tempFiles.add(middleClassfiles.get(i));
            final IBundleCoverage oldBundle = analyze(oldLoader.getExecutionDataStore(),
                    out, tempFiles);// 读取class文件
            mergeProbes(oldBundle, bundle, getOldDiffData().get(i), CoverageBuilder.classInfos, loader.getExecutionDataStore());
        }

        // 在运行analyze之前修改loader数据是可以，但是之后是无效的，所以目前是先通过mergeprobes修改了loader，再重新analyze
        final IBundleCoverage bundlenew = analyze(loader.getExecutionDataStore(),
                out, classfiles);// 读取class文件

        writeReports(bundlenew, loader, out);
        return 0;
    }

    public List<List<ClassInfoDto>> getOldDiffData() {
        Gson gson = new Gson();
        List<List<ClassInfoDto>> results = new ArrayList<>();
        for (String diffCode : this.oldDiffCode) {
            List<ClassInfoDto> classInfos = gson.fromJson(diffCode,
                    new TypeToken<List<ClassInfoDto>>() {
                    }.getType());
            results.add(classInfos);
        }
        return results;
    }

    private ExecFileLoader loadExecutionData(final PrintWriter out, List<File> execfiles)
            throws IOException {
        final ExecFileLoader loader = new ExecFileLoader();
        if (execfiles.isEmpty()) {
            out.println("[WARN] No execution data files provided.");
        } else {
            for (final File file : execfiles) {
                out.printf("[INFO] Loading execution data file %s.%n",
                        file.getAbsolutePath());
                loader.load(file);
            }
        }
        return loader;
    }

    private IBundleCoverage analyze(final ExecutionDataStore data,
                                    final PrintWriter out, List<File> classfiles) throws IOException {
        CoverageBuilder builder;
        // 如果有增量参数将其设置进去
        if (null != this.diffCodeFiles) {
            builder = new CoverageBuilder(
                    JsonReadUtil.readJsonToString(this.diffCodeFiles));
        } else if (null != this.diffCode) {
            builder = new CoverageBuilder(this.diffCode);
        } else {
            builder = new CoverageBuilder();
        }
        final Analyzer analyzer = new Analyzer(data, builder);
        for (final File f : classfiles) {
            analyzer.analyzeAll(f);
        }
        printNoMatchWarning(builder.getNoMatchClasses(), out);
        return builder.getBundle(name);
    }

    private void printNoMatchWarning(final Collection<IClassCoverage> nomatch,
                                     final PrintWriter out) {
        if (!nomatch.isEmpty()) {
            out.println(
                    "[WARN] Some classes do not match with execution data.");
            out.println(
                    "[WARN] For report generation the same class files must be used as at runtime.");
            for (final IClassCoverage c : nomatch) {
                out.printf(
                        "[WARN] Execution data for class %s does not match.%n",
                        c.getName());
            }
        }
    }

    private void writeReports(final IBundleCoverage bundle,
                              final ExecFileLoader loader, final PrintWriter out)
            throws IOException {
        out.printf("[INFO] Analyzing %s classes.%n",
                Integer.valueOf(bundle.getClassCounter().getTotalCount()));
        final IReportVisitor visitor = createReportVisitor();
        visitor.visitInfo(loader.getSessionInfoStore().getInfos(),
                loader.getExecutionDataStore().getContents());
        visitor.visitBundle(bundle, getSourceLocator());
        visitor.visitEnd();
    }

    private IReportVisitor createReportVisitor() throws IOException {
        final List<IReportVisitor> visitors = new ArrayList<IReportVisitor>();

        if (xml != null) {
            final XMLFormatter formatter = new XMLFormatter();
            visitors.add(formatter.createVisitor(new FileOutputStream(xml)));
        }

        if (csv != null) {
            final CSVFormatter formatter = new CSVFormatter();
            visitors.add(formatter.createVisitor(new FileOutputStream(csv)));
        }

        if (html != null) {
            final HTMLFormatter formatter = new HTMLFormatter();
            visitors.add(
                    formatter.createVisitor(new FileMultiReportOutput(html)));
        }

        return new MultiReportVisitor(visitors);
    }

    private ISourceFileLocator getSourceLocator() {
        final MultiSourceFileLocator multi = new MultiSourceFileLocator(
                tabwidth);
        for (final File f : sourcefiles) {
            multi.add(new DirectorySourceFileLocator(f, encoding, tabwidth));
        }
        return multi;
    }

    /**
     * 从base commit和最新commit 的diff数据中 剔除变化的数据，获取未变的diff数据，方便后续的探针数据累加
     *
     * @param finalRes base commit和最新的commit 之间的diff数据
     * @param temps    base commit之后的commit 和最新的commit 之间的diff数据
     * @return
     */
    public List<ClassInfoDto> getNoChangeData(List<ClassInfoDto> finalRes, List<ClassInfoDto> temps) {
        // 从最终的集合中剔除掉变化的
        List<ClassInfoDto> result = new ArrayList();
        for (ClassInfoDto finalClass : finalRes) {
            ClassInfoDto newClass = new ClassInfoDto();
            // 有相同的变化的类
            if (temps.contains(finalClass)) {
                for (ClassInfoDto temp : temps) {
                    if (temp.equals(finalClass)) {
                        newClass.setClassFile(temp.getClassFile());
                        // 遍历所有方法，保存未变化的方法
                        for (MethodInfoDto finalMethod : finalClass.getMethodInfos()) {
                            if (!temp.getMethodInfos().contains(finalMethod)) {
                                newClass.getMethodInfos().add(finalMethod);
                            }
                        }
                    }
                }
            } else {
                newClass = finalClass;
            }

            // 只有类中有未变的方法，才会添加到最终result中
            if (newClass.getMethodInfos().size() != 0) {
                result.add(newClass);
            }
        }
        return result;
    }

    /**
     * 完全没有执行到的类，exec文件中没有类的probe的数据，就没有finalClasses
     * MethodCoverageImpl 的offset是开始行，LineImpl[] lines是函数具体的行
     */
    /**
     * 计算未变的方法，在根据MethodCoverageImpl中记录的探针数组的开始结束位置，累加相同函数的探针数据
     *
     * @param middleBundle base和final中间的commit 生成的bundle
     * @param finalBundle  final commit生成的bundle
     * @param middleDiff   base和final中间的commit 生成的diff数据
     * @param finalDiff    final commit生成的diff数据
     */
    public void mergeProbes(IBundleCoverage middleBundle, IBundleCoverage finalBundle, List<ClassInfoDto> middleDiff,
                            List<ClassInfoDto> finalDiff, final ExecutionDataStore executionDataStore) {
        List<ClassInfoDto> noChangeList = getNoChangeData(finalDiff, middleDiff);

        //com/lphtsccft/zhangle/foundation/framework/modular/ModularStartupImpl_getDeviceId_()Ljava/lang/String;
        //com/lphtsccft/zhangle/main/MainActivity$FlipperClickListener_onClick_(Landroid/view/View;)V
        // 将oldbundle 的数据以key是方法签名，value是MethodCoverageImpl对象来存储
        final Collection<IPackageCoverage> middlePackages = middleBundle.getPackages();
        HashMap<String, MethodCoverageImpl> middleMethods = new HashMap();
        // 构建类名+方法名+方法参数签名作为key，方便后续查找
        for (IPackageCoverage p : middlePackages) {
            final Collection<IClassCoverage> middleClasses = p.getClasses();
            for (IClassCoverage c : middleClasses) {
                final Collection<IMethodCoverage> methods = c.getMethods();
                for (IMethodCoverage methodCoverage : methods) {
                    middleMethods.put(c.getName() + "_" + methodCoverage.getName() + "_" + methodCoverage.getDesc(), (MethodCoverageImpl) methodCoverage);
                }
            }
        }
        // 遍历新的bundle数据，累加旧的bundle数据，根据类，方法名来判断，loader中是probe原数据，bundle中有每个函数的开始结束位置，根据位置来累加。
        final Collection<IPackageCoverage> finalPackages = finalBundle.getPackages();

        // 遍历最新commit的包
        for (IPackageCoverage p : finalPackages) {
            final Collection<IClassCoverage> finalClasses = p.getClasses();
            //遍历最新commit的类
            for (IClassCoverage c : finalClasses) {
                final Collection<IMethodCoverage> finalMethods = c.getMethods();
                //遍历最新commit的方法
                for (IMethodCoverage methodCoverage : finalMethods) {
                    // commit base 到commit最新 中未变化的方法
                    if (CodeDiffUtil.checkMethodIn(c.getName(), methodCoverage.getName(), methodCoverage.getDesc(), noChangeList)) {
                        boolean[] finalProbes = ((ClassCoverageImpl) c).probes;
                        int finalStart = ((MethodCoverageImpl) methodCoverage).getProbeStart();
                        int finalEnd = ((MethodCoverageImpl) methodCoverage).getProbeEnd();
                        MethodCoverageImpl oldMethod = middleMethods.get(c.getName() + "_" + methodCoverage.getName() + "_" + methodCoverage.getDesc());
                        boolean[] middleProbes = oldMethod.getProbes();
                        int middleStart = oldMethod.getProbeStart();
                        int middleEnd = oldMethod.getProbeEnd();
                        if (middleEnd - middleStart != finalEnd - finalStart || middleProbes == null) {
                            System.out.println("Error: probes for method start and end is error.");
                            break;
                        }

                        /**
                         * 存在最终的exec中没有执行某个类，则这个类的probes是空，得单独生成一个probes数组对象给到ClassCoverageImpl
                         */
                        if (finalProbes == null) {
                            finalProbes = new boolean[((ClassCoverageImpl) c).getProbesCount()];
//                            ((MethodCoverageImpl) methodCoverage).probes = finalProbes;
                            ((ClassCoverageImpl) c).probes = finalProbes;
                            executionDataStore.getNames().add(c.getName());
                            executionDataStore.getEntries().put(c.getId(), new ExecutionData(c.getId(), c.getName(), finalProbes));
                        }
                        // probe边界是[)
                        for (int i = finalStart, j = middleStart; i < finalEnd; i++, j++) {
                            if (middleProbes[j] == true) {
                                finalProbes[i] = true;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 单个java文件可能编译长多个class文件，例如Main.java，编译成Main$1.class,Main$2.class等
     * 内部类可能的命名：Main$listener$1,Main$listener
     * Main$1$2$3??
     * <p>
     * 匿名内部类生成的class文件无法一一匹配，这个有点复杂
     */
    public String getClassName(String OriginName) {
        String[] names = OriginName.split("\\$");
        String endName = names[names.length - 1];
        if (endName.charAt(0) >= '0' && endName.charAt(0) <= '9') {
            return OriginName.substring(0, OriginName.length() - endName.length() - 1);
        }
        return OriginName;
    }

}
