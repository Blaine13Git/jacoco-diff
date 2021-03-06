package org.jacoco.core.internal.diff;


import org.apache.commons.codec.binary.Base64;
import org.eclipse.jdt.core.dom.*;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AST编译java源文件
 */
public class ASTGenerator {
    private String javaText;
    private CompilationUnit compilationUnit;

    public ASTGenerator(String javaText) {
        this.javaText = javaText;
        this.initCompilationUnit();
    }

    /**
     * 解析源码获取编译单元
     */
    private void initCompilationUnit() {
        final ASTParser astParser = ASTParser.newParser(AST.JLS13);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setSource(javaText.toCharArray());
        compilationUnit = (CompilationUnit) astParser.createAST(null);
    }

    /**
     * 获取包名
     *
     * @return
     */
    public String getPackageName() {
        if (compilationUnit == null) {
            return "";
        }
        PackageDeclaration packageDeclaration = compilationUnit.getPackage();
        if (packageDeclaration == null) {
            return "";
        }
        String packageName = packageDeclaration.getName().toString();
        return packageName;
    }

    /**
     * 获取类
     *
     * @return
     */
    public TypeDeclaration getJavaClass() {
        if (compilationUnit == null) {
            return null;
        }
        TypeDeclaration typeDeclaration = null;
        final List<?> types = compilationUnit.types();
        for (final Object type : types) {
            if (type instanceof TypeDeclaration) {
                typeDeclaration = (TypeDeclaration) type;
                break;
            }
        }
        return typeDeclaration;
    }

    /**
     * 获取类中所有方法
     *
     * @return
     */
    public MethodDeclaration[] getMethods() {
        TypeDeclaration typeDec = getJavaClass();
        if (typeDec == null) {
            return new MethodDeclaration[]{};
        }
        MethodDeclaration[] methodDec = typeDec.getMethods();
        return methodDec;
    }

    /**
     * 获取新增类中的所有方法信息
     *
     * @return
     */
    public List<MethodInfo> getMethodInfoList() {
        MethodDeclaration[] methodDeclarations = getMethods();
        List<MethodInfo> methodInfoList = new ArrayList<MethodInfo>();
        for (MethodDeclaration method : methodDeclarations) {
            MethodInfo methodInfo = new MethodInfo();
            setMethodInfo(methodInfo, method);
            methodInfoList.add(methodInfo);
        }
        return methodInfoList;
    }

    /**
     * 获取修改类型的类的信息以及其中的所有方法，排除接口类
     *
     * @param methodInfos
     * @param addLines
     * @param delLines
     * @return
     */
    public ClassInfo getClassInfo(List<MethodInfo> methodInfos, List<int[]> addLines, List<int[]> delLines) {
        TypeDeclaration typeDec = getJavaClass();
        if (typeDec == null || typeDec.isInterface()) {
            return null;
        }
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassName(getJavaClass().getName().toString());
        classInfo.setPackages(getPackageName());
        classInfo.setMethodInfos(methodInfos);
        classInfo.setAddLines(addLines);
        classInfo.setDelLines(delLines);
        classInfo.setType("REPLACE");
        return classInfo;
    }

    /**
     * 获取新增类型的类的信息以及其中的所有方法，排除接口类
     *
     * @return
     */
    public ClassInfo getClassInfo() {
        TypeDeclaration typeDec = getJavaClass();
        if (typeDec == null || typeDec.isInterface()) {
            return null;
        }
        MethodDeclaration[] methodDeclarations = getMethods();
        ClassInfo classInfo = new ClassInfo();
        classInfo.setClassName(getJavaClass().getName().toString());
        classInfo.setPackages(getPackageName());
        classInfo.setType("ADD");
        List<MethodInfo> methodInfoList = new ArrayList<MethodInfo>();
        for (MethodDeclaration method : methodDeclarations) {
            MethodInfo methodInfo = new MethodInfo();
            setMethodInfo(methodInfo, method);
            methodInfoList.add(methodInfo);
        }
        classInfo.setMethodInfos(methodInfoList);
        return classInfo;
    }

    /**
     * 获取修改中的方法
     *
     * @param methodDeclaration
     * @return
     */
    public MethodInfo getMethodInfo(MethodDeclaration methodDeclaration) {
        MethodInfo methodInfo = new MethodInfo();
        setMethodInfo(methodInfo, methodDeclaration);
        return methodInfo;
    }

    private void setMethodInfo(MethodInfo methodInfo, MethodDeclaration methodDeclaration) {
        methodInfo.setMd5(MD5Encode(methodDeclaration.toString()));
        methodInfo.setMethodName(methodDeclaration.getName().toString());
        methodInfo.setParameters(methodDeclaration.parameters().toString());
    }

    /**
     * 计算方法的MD5的值
     *
     * @param s
     * @return
     */
    public static String MD5Encode(String s) {
        String md5String = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = Base64.encodeBase64(md5.digest(s.getBytes("utf-8")));
            md5String = new String(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return md5String;
    }

    /**
     * 判断方法是否存在
     *
     * @param method     新分支的方法
     * @param methodsMap master分支的方法
     * @return
     */
    public static boolean isMethodExist(final MethodDeclaration method, final Map<String, MethodDeclaration> methodsMap) {
        // 方法名+参数一致才一致
        if (!methodsMap.containsKey(method.getName().toString() + method.parameters().toString())) {
            return false;
        }
        return true;
    }

    /**
     * 判断方法是否一致
     *
     * @param method1
     * @param method2
     * @return
     */
    public static boolean isMethodTheSame(final MethodDeclaration method1, final MethodDeclaration method2) {
        if (MD5Encode(method1.toString()).equals(MD5Encode(method2.toString()))) {
            return true;
        }
        return false;
    }
}
