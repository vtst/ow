package net.vtst.ow.eclipse.js.closure.builder.test;

import static org.junit.Assert.*;

import net.vtst.ow.eclipse.js.closure.OwJsClosurePlugin;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class ClosureBuilderTest {

    protected static WorkspaceDriver driver = null;

    private static Bundle bundle = OwJsClosurePlugin.getDefault().getBundle();

    @BeforeClass
    public static void setUp() {

        if (driver == null) {
            driver = new WorkspaceDriver();
            driver.openEmptyWorkspace();
        }
        driver.resetWorkspace();
    }


    @After
    public void reset() {

        driver.resetWorkspace();
    }


    @AfterClass
    public static void tearDown() {

        driver.close();
    }

    // incremental projectbuilder must reimplement at least build
    // returns referenced projects
    // incremental build + autobuild -> getDelta and don't recompile everything
    // full build -> build everything
    // build is robust with errors, and can be cancelled any time
    // implements clean
    // delete all derived resources from previous builds
    // remove all problem markers

    @Test
    public void testBuild() throws Exception {

        String projectName = "project";
        driver.addProject(projectName);
        // env.addExternalJars(projectPath, Util.getJavaClassLibs());
        // env.addGroovyJars(projectPath);
        driver.fullBuild(projectName);
        
        
        driver.addSourceFolder("js", projectName);
        // // remove old package fragment root so that names don't collide
        //            env.removePackageFragmentRoot(projectPath, ""); //$NON-NLS-1$
        //
        //            IPath root = env.addPackageFragmentRoot(projectPath, "src"); //$NON-NLS-1$
        //            env.setOutputFolder(projectPath, "bin"); //$NON-NLS-1$
        //
        // // JDTResolver.recordInstances = true;
        //
        // env.addGroovyClass(root, "", "BuildSettings",
        // "import groovy.transform.CompileStatic\n"+
        // "\n"+
        // "class BuildSettings  {\n"+
        // "\n"+
        // "   List<File> compileDependencies = []\n"+
        // "   List<File> defaultCompileDependencies = []\n"+
        // "\n"+
        // "    @CompileStatic\n"+
        // "    void getCompileDependencies() {\n"+
        // "        compileDependencies += defaultCompileDependencies\n"+
        // "    }\n"+
        // "\n"+
        // "}\n");
        driver.incrementalBuild(projectName);

        ClosureProjectMarkerRetriever retriever = new ClosureProjectMarkerRetriever(driver);


        String[] markers =
                new String[] {"JavaScript Closure Builder",
                        "JavaScript Closure Compiler",
                        "JavaScript Closure Linter"};

        assertTrue(retriever.getMarkers(markers).isEmpty());
    }
 
    //from groovy : /org.eclipse.jdt.groovy.core.tests.builder/src/org/eclipse/jdt/core/groovy/tests/builder/BasicGroovyBuildTests.java
 // build hello world and run it 
//    public void testBuildJavaHelloWorld() throws JavaModelException {
//        IPath projectPath = driver.addProject("Project"); //$NON-NLS-1$
//        driver.addExternalJars(projectPath, Util.getJavaClassLibs());
//        fullBuild(projectPath);
//        // remove old package fragment root so that names don't collide
//        driver.removePackageFragmentRoot(projectPath, ""); //$NON-NLS-1$
//
//        IPath root = driver.addPackageFragmentRoot(projectPath, "src"); //$NON-NLS-1$
//        driver.setOutputFolder(projectPath, "bin"); //$NON-NLS-1$
//
//        driver.addClass(root, "p1", "Hello", "package p1;\n"
//                + "public class Hello {\n"
//                + "   public static void main(String[] args) {\n"
//                + "      System.out.println(\"Hello world\");\n" + "   }\n"
//                + "}\n");
//
//        incrementalBuild(projectPath);
//        expectingCompiledClassesV("p1.Hello");
//        expectingNoProblems();
//        executeClass(projectPath, "p1.Hello", "Hello world", "");
//
//    }

}
