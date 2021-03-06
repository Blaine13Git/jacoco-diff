/*******************************************************************************
 * Copyright (c) 2009, 2020 Mountainminds GmbH & Co. KG and Contributors
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
package org.jacoco.core.analysis;

import java.util.*;

import org.jacoco.core.internal.analysis.BundleCoverageImpl;
import org.jacoco.core.internal.analysis.SourceFileCoverageImpl;
import org.jacoco.core.internal.diff.ClassInfo;
import org.jacoco.core.internal.diff.CodeDiff;

/**
 * Builder for hierarchical {@link ICoverageNode} structures from single
 * {@link IClassCoverage} nodes. The nodes are feed into the builder through its
 * {@link ICoverageVisitor} interface. Afterwards the aggregated data can be
 * obtained with {@link #getClasses()}, {@link #getSourceFiles()} or
 * {@link #getBundle(String)} in the following hierarchy:
 *
 * <pre>
 * {@link IBundleCoverage}
 * +-- {@link IPackageCoverage}*
 *     +-- {@link IClassCoverage}*
 *     +-- {@link ISourceFileCoverage}*
 * </pre>
 */
public class CoverageBuilder implements ICoverageVisitor {

    private final Map<String, IClassCoverage> classes;

    private final Map<String, ISourceFileCoverage> sourcefiles;

    public static List<ClassInfo> classInfos;

    /**
     * Create a new builder.
     */
    public CoverageBuilder() {
        this.classes = new HashMap<>();
        this.sourcefiles = new HashMap<>();
    }

    /**
     * 分支之间比较
     * @param gitPath			本地git路径
     * @param newBranchName		新分支名称
     * @param oldBranchName		对比分支名称
     */
    public CoverageBuilder(String gitPath, String newBranchName, String oldBranchName) {
        this.classes = new HashMap<>();
        this.sourcefiles = new HashMap<>();
        if (classInfos == null || classInfos.isEmpty()){
            classInfos = CodeDiff.diffBranchToBranch(gitPath, newBranchName, oldBranchName);
        }
    }

    /**
     * 分支和master比较
     * @param gitPath		本地git路径
     * @param branchName	新分支名称
     */
    public CoverageBuilder(String gitPath, String branchName) {
        this.classes = new HashMap<>();
        this.sourcefiles = new HashMap<>();
        if (classInfos == null || classInfos.isEmpty()){
            classInfos = CodeDiff.diffBranchToMaster(gitPath, branchName);
        }
    }

    /**
     * Returns all class nodes currently contained in this builder.
     *
     * @return all class nodes
     */
    public Collection<IClassCoverage> getClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * Returns all source file nodes currently contained in this builder.
     *
     * @return all source file nodes
     */
    public Collection<ISourceFileCoverage> getSourceFiles() {
        return Collections.unmodifiableCollection(sourcefiles.values());
    }

    /**
     * Creates a bundle from all nodes currently contained in this bundle.
     *
     * @param name Name of the bundle
     * @return bundle containing all classes and source files
     */
    public IBundleCoverage getBundle(final String name) {
        return new BundleCoverageImpl(name, classes.values(),
                sourcefiles.values());
    }

    /**
     * Returns all classes for which execution data does not match.
     *
     * @return collection of classes with non-matching execution data
     * @see IClassCoverage#isNoMatch()
     */
    public Collection<IClassCoverage> getNoMatchClasses() {
        final Collection<IClassCoverage> result = new ArrayList<IClassCoverage>();
        for (final IClassCoverage c : classes.values()) {
            if (c.isNoMatch()) {
                result.add(c);
            }
        }
        return result;
    }

    // === ICoverageVisitor ===

    public void visitCoverage(final IClassCoverage coverage) {
        final String name = coverage.getName();
        final IClassCoverage dup = classes.put(name, coverage);
        if (dup != null) {
            if (dup.getId() != coverage.getId()) {
                throw new IllegalStateException("Can't add different class with same name: " + name);
            }
        } else {
            final String source = coverage.getSourceFileName();
            if (source != null) {
                final SourceFileCoverageImpl sourceFile = getSourceFile(source, coverage.getPackageName());
                sourceFile.increment(coverage);
            }
        }
    }

    private SourceFileCoverageImpl getSourceFile(final String filename, final String packagename) {
        final String key = packagename + '/' + filename;
        SourceFileCoverageImpl sourcefile = (SourceFileCoverageImpl) sourcefiles.get(key);
        if (sourcefile == null) {
            sourcefile = new SourceFileCoverageImpl(filename, packagename);
            sourcefiles.put(key, sourcefile);
        }
        return sourcefile;
    }

}
