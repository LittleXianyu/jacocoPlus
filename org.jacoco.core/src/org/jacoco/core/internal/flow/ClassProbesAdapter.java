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
package org.jacoco.core.internal.flow;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.internal.analysis.ClassAnalyzer;
import org.jacoco.core.internal.diff.CodeDiffUtil;
import org.jacoco.core.internal.instr.InstrSupport;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;

/**
 * A {@link org.objectweb.asm.ClassVisitor} that calculates probes for every
 * method.
 */
public class ClassProbesAdapter extends ClassVisitor
		implements IProbeIdGenerator {

	private static final MethodProbesVisitor EMPTY_METHOD_PROBES_VISITOR = new MethodProbesVisitor() {
	};

	private final ClassProbesVisitor cv;

	private final boolean trackFrames;

	private int counter = 0;

	private String name;

	/**
	 * Creates a new adapter that delegates to the given visitor.
	 *
	 * @param cv
	 *            instance to delegate to
	 * @param trackFrames
	 *            if <code>true</code> stackmap frames are tracked and provided
	 */
	public ClassProbesAdapter(final ClassProbesVisitor cv,
			final boolean trackFrames) {
		super(InstrSupport.ASM_API_VERSION, cv);
		this.cv = cv;
		this.trackFrames = trackFrames;
	}

	@Override
	public void visit(final int version, final int access, final String name,
			final String signature, final String superName,
			final String[] interfaces) {
		this.name = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}

	@Override
	public final MethodVisitor visitMethod(final int access, final String name,
			final String desc, final String signature,
			final String[] exceptions) {
		final MethodProbesVisitor methodProbes;
		final MethodProbesVisitor mv = cv.visitMethod(access, name, desc,
				signature, exceptions);
		if (null != mv) {
			// 增量代码，有点绕，由于参数定义成final,无法第二次指定,代码无法简化
			if (null != CoverageBuilder.classInfos
					&& !CoverageBuilder.classInfos.isEmpty()) {
				if (CodeDiffUtil.checkMethodIn(this.name, name, desc,CoverageBuilder.classInfos)) {
					System.out.println("xianyu0 classname: "+this.name+"    methodName: "+name+"   start count: "+counter);
					methodProbes = mv;
					if(methodProbes instanceof ClassAnalyzer.InnerMethodAnalyzer){
						((ClassAnalyzer.InnerMethodAnalyzer)methodProbes).setCounterStart(counter);
					}
				} else {
					methodProbes = EMPTY_METHOD_PROBES_VISITOR;
//					System.out.println("xianyu0 classname: "+this.name+"    methodName: "+name+"   start count: "+counter);
//					methodProbes = mv;
//					if(methodProbes instanceof ClassAnalyzer.InnerMethodAnalyzer){
//						((ClassAnalyzer.InnerMethodAnalyzer)methodProbes).setCounterStart(counter);
//					}
				}
			} else {
				methodProbes = mv;
			}
		} else {
			methodProbes = EMPTY_METHOD_PROBES_VISITOR;
		}

		return new MethodSanitizer(null, access, name, desc, signature,
				exceptions) {

			@Override
			public void visitEnd() {
				super.visitEnd();
				LabelFlowAnalyzer.markLabels(this);
				final MethodProbesAdapter probesAdapter = new MethodProbesAdapter(
						methodProbes, ClassProbesAdapter.this);
				if (trackFrames) {
					final AnalyzerAdapter analyzer = new AnalyzerAdapter(
							ClassProbesAdapter.this.name, access, name, desc,
							probesAdapter);
					probesAdapter.setAnalyzer(analyzer);
					methodProbes.accept(this, analyzer);
				} else {
					methodProbes.accept(this, probesAdapter);
				}
			}

		};
	}

	@Override
	public void visitEnd() {
		cv.visitTotalProbeCount(counter);
		super.visitEnd();
	}

	// === IProbeIdGenerator ===

	public int nextId() {
		return counter++;
	}

	public int getCounter() {
		return counter;
	}
}
