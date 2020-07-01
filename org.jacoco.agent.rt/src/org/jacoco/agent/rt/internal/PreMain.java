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
package org.jacoco.agent.rt.internal;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.jacoco.core.runtime.AgentOptions;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.InjectedClassRuntime;
import org.jacoco.core.runtime.ModifiedSystemClassRuntime;

/**
 * The agent which is referred as the <code>Premain-Class</code>. The agent
 * configuration is provided with the agent parameters in the command line.
 */
public final class PreMain {

    private PreMain() {
        // no instances
    }

    /**
     * JVM启动时添加代理
     *
     * @param options
     * @param inst
     * @throws Exception
     */
    public static void premain(final String options, final Instrumentation inst) throws Exception {

        // 重定向输出到指定文件
        if (false) {
            Date date = new Date();
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            String formatDate = dateFormat.format(date);

            String traceFilePath = "";
            String traceFile;

            String projectPath = System.getProperty("user.dir");

            String projectName = projectPath.substring(projectPath.lastIndexOf("/") + 1);

            if (null == traceFilePath || traceFilePath.length() == 0) {
                traceFile = projectPath + "/" + formatDate + "_" + projectName + "_Trace.log";
                redirectOutPut(traceFile);
            } else {
                traceFile = traceFilePath + "/" + formatDate + "_" + projectName + "_Trace.log";
                redirectOutPut(traceFile);
            }

            System.out.println("======================日志被重定向到 >> " + traceFile + "======================");
        }

        System.out.println("======================init by premain======================");

        final AgentOptions agentOptions = new AgentOptions(options);

        final Agent agent = Agent.getInstance(agentOptions);

        final IRuntime runtime = createRuntime(inst);

        runtime.startup(agent.getData());

        inst.addTransformer(new CoverageTransformer(runtime, agentOptions, IExceptionLogger.SYSTEM_ERR));
    }

    /**
     * JVM启动后添加代理
     *
     * @param options
     * @param instrumentation
     * @throws Exception
     */
    public static void agentmain(String options, Instrumentation instrumentation) throws Exception {

        System.out.println("======================init by agentmain======================");

        final AgentOptions agentOptions = new AgentOptions(options);

        final Agent agent = Agent.getInstance(agentOptions);

        final IRuntime runtime = createRuntime(instrumentation);

        runtime.startup(agent.getData());

        instrumentation.addTransformer(new CoverageTransformer(runtime, agentOptions, IExceptionLogger.SYSTEM_ERR));
    }

    private static IRuntime createRuntime(final Instrumentation inst) throws Exception {

        if (redefineJavaBaseModule(inst)) {
            return new InjectedClassRuntime(Object.class, "$JaCoCo");
        }

        return ModifiedSystemClassRuntime.createFor(inst,
                "java/lang/UnknownError");
    }

    /**
     * Opens {@code java.base} module for {@link InjectedClassRuntime} when
     * executed on Java 9 JREs or higher.
     *
     * @return <code>true</code> when running on Java 9 or higher,
     * <code>false</code> otherwise
     * @throws Exception if unable to open
     */
    private static boolean redefineJavaBaseModule(final Instrumentation instrumentation) throws Exception {
        try {
            Class.forName("java.lang.Module");
        } catch (final ClassNotFoundException e) {
            return false;
        }

        Instrumentation.class.getMethod(
                "redefineModule", //
                Class.forName("java.lang.Module"), //
                Set.class, //
                Map.class, //
                Map.class, //
                Set.class, //
                Map.class //
        ).invoke(
                instrumentation, // instance
                getModule(Object.class), // module
                Collections.emptySet(), // extraReads
                Collections.emptyMap(), // extraExports
                Collections.singletonMap("java.lang", Collections.singleton(getModule(InjectedClassRuntime.class))), // extraOpens
                Collections.emptySet(), // extraUses
                Collections.emptyMap() // extraProvides
        );
        return true;
    }

    /**
     * @return {@code cls.getModule()}
     */
    private static Object getModule(final Class<?> cls) throws Exception {
        return Class.class.getMethod("getModule").invoke(cls);
    }


    /**
     * 重定向输出
     *
     * @param filePath
     */
    public static void redirectOutPut(String filePath) {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(filePath, true);
            PrintStream printStream = new PrintStream(fileOutputStream);
            System.setOut(printStream);
            System.setErr(printStream);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
