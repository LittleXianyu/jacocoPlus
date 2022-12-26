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
package org.jacoco.core.internal.analysis;

import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.IMethodCoverage;

/**
 * Implementation of {@link IMethodCoverage}.
 */
public class MethodCoverageImpl extends SourceNodeImpl
        implements IMethodCoverage {

    private final String desc;

    private final String signature;

    private int probeStart;

    private int probeEnd;

    private final boolean[] probes;

    /**
     * Creates a method coverage data object with the given parameters.
     *
     * @param name      name of the method
     * @param desc      method descriptor
     * @param signature generic signature or <code>null</code>
     */
    public MethodCoverageImpl(final String name, final String desc,
                              final String signature, boolean[] probes) {
        super(ElementType.METHOD, name);
        this.desc = desc;
        this.signature = signature;
        this.probes = probes;
    }

    @Override
    public void increment(final ICounter instructions, final ICounter branches,
                          final int line) {
        super.increment(instructions, branches, line);
        // Additionally increment complexity counter:
        if (branches.getTotalCount() > 1) {
            final int c = Math.max(0, branches.getCoveredCount() - 1);
            final int m = Math.max(0, branches.getTotalCount() - c - 1);
            this.complexityCounter = this.complexityCounter.increment(m, c);
        }
    }

    /**
     * This method must be called exactly once after all instructions and
     * branches have been incremented for this method coverage node.
     */
    public void incrementMethodCounter() {
        final ICounter base = this.instructionCounter.getCoveredCount() == 0
                ? CounterImpl.COUNTER_1_0
                : CounterImpl.COUNTER_0_1;
        this.methodCounter = this.methodCounter.increment(base);
        this.complexityCounter = this.complexityCounter.increment(base);
    }

    // === IMethodCoverage implementation ===

    public String getDesc() {
        return desc;
    }

    public String getSignature() {
        return signature;
    }

    public int getProbeStart() {
        return probeStart;
    }

    public void setProbeStart(int probeStart) {
        this.probeStart = probeStart;
    }

    public int getProbeEnd() {
        return probeEnd;
    }

    public void setProbeEnd(int probeEnd) {
        this.probeEnd = probeEnd;
    }

    public boolean[] getProbes() {
        return probes;
    }
}
